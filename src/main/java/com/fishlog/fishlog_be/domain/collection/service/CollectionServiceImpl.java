package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.ClassifyResponse;
import com.fishlog.fishlog_be.domain.collection.dto.DexEntryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.FishCandidateResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.dto.VerifyResponse;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.policy.CatchRecordPolicy;
import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import com.fishlog.fishlog_be.domain.collection.repository.CatchStats;
import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.global.ai.AiErrorCode;
import com.fishlog.fishlog_be.global.ai.FishClassifyClient;
import com.fishlog.fishlog_be.global.ai.dto.PredictResponse;
import com.fishlog.fishlog_be.global.ai.dto.PredictionItem;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.s3.PathName;
import com.fishlog.fishlog_be.global.s3.S3Service;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionServiceImpl implements CollectionService {

  private final CatchRecordRepository catchRecordRepository;
  // 도메인 간 접근은 상대 도메인의 service 인터페이스로만 한다(fish 의 repository·entity 직접 접근 금지).
  private final FishService fishService;
  private final FishClassifyClient fishClassifyClient;
  private final S3Service s3Service;
  // 탈퇴 정리를 collection 도메인 안에서 마무리하기 위한 위임 대상(같은 도메인의 형제 서비스).
  private final CustomCatchService customCatchService;

  @Override
  public CatchRecordResponse getMyCatch(Long userId, Long fishId) {
    // 서식지는 기록이 아니라 어종의 속성이라 어종을 먼저 조회한다 — 안 잡은 어종은 기록이 0건이라
    // 거기서 서식지를 끌어올 수 없다. 이 조회 때문에 없는 fishId 는 빈 결과가 아니라 F001(404)가 된다.
    Fish fish = fishService.getFishEntity(fishId);
    // 횟수·최대 크기는 따로 집계한다 — 아래에서 사진을 4장으로 자르므로 리스트로는 둘 다 알 수 없다.
    // (최대 크기가 5번째로 오래된 기록에 있어도 잡혀야 한다.) 같은 스캔을 두 번 돌지 않도록 한 쿼리로 받는다.
    CatchStats stats = catchRecordRepository.findStatsByUserIdAndFishId(userId, fishId);
    // 최신순 상위 N건만 조회. 안 잡았으면 빈 리스트 → 200 + catchCount 0 + recentCatches [].
    List<CatchRecord> recentRecords =
        catchRecordRepository.findByUserIdAndFish_IdOrderByCreatedAtDescIdDesc(
            userId, fishId, PageRequest.of(0, CatchRecordPolicy.RECENT_PHOTO_LIMIT));
    return CatchRecordResponse.of(
        fish.getHabitat(), (int) stats.getCatchCount(), stats.getMaxSize(), recentRecords);
  }

  @Override
  public MyDexResponse getMyDex(Long userId) {
    // 1) 전체 도감(수집 대상 어종)을 fish 서비스에서 그대로 가져온다(순서·집합 동일).
    List<FishSummaryResponse> dex = fishService.getFishList(null).fishes();
    // 2) 내가 잡은 어종 id 집합(중복 제거) → 칸마다 O(1) 판정용.
    Set<Long> caughtIds = new HashSet<>(catchRecordRepository.findDistinctCaughtFishIds(userId));
    // 3) 두 결과를 병합해 각 칸에 caught 를 덧입힌다(N+1 없이 메모리 조합).
    List<DexEntryResponse> entries =
        dex.stream().map(fish -> DexEntryResponse.of(fish, caughtIds.contains(fish.id()))).toList();
    return MyDexResponse.of(entries);
  }

  @Override
  @Transactional
  public void deleteMyRecords(Long userId) {
    catchRecordRepository.deleteByUserId(userId);
    // 도감 외 어종 기록도 같은 사용자의 도감 데이터다 — 여기서 함께 지워야 user 도메인이 테이블마다
    // 호출을 늘리지 않아도 되고, 정리 누락으로 조회되지 않는 고아 사진이 남지 않는다.
    customCatchService.deleteMyRecords(userId);
  }

  @Override
  public ClassifyResponse classify(MultipartFile image) {
    // 모델 서버에 닿지 못한 경우만 empty 다(사진 자체가 문제면 클라이언트가 이미 AiErrorCode 4xx 로 예외를 던진다).
    // 여기서 503 으로 끊어 클라이언트가 "목록에서 직접 선택" 대안 경로로 넘어가게 한다.
    PredictResponse prediction =
        fishClassifyClient
            .predict(image)
            .orElseThrow(() -> new CustomException(AiErrorCode.CLASSIFY_UNAVAILABLE));

    List<FishCandidateResponse> candidates = toCandidates(prediction.safePredictions());
    log.info(
        "어종 분류 완료: model={}, uncertain={}, top1={}, 후보 {}건",
        prediction.modelVersion(),
        prediction.uncertain(),
        prediction.top1Confidence(),
        candidates.size());
    return ClassifyResponse.of(prediction.modelVersion(), prediction.uncertain(), candidates);
  }

  @Override
  @Transactional
  public VerifyResponse verify(
      Long userId, Long fishId, Double size, String location, MultipartFile image) {
    CatchRecordPolicy.validateSize(size);
    // 정규화를 업로드보다 먼저 끝낸다 — 길이 초과로 400을 낼 거라면 S3에 올리기 전이어야 한다.
    String catchLocation = CatchRecordPolicy.normalizeLocation(location);
    // 어종 확인을 업로드보다 먼저 한다 — 없는 어종이면 S3에 고아 객체를 남기지 않고 404로 끝난다.
    Fish fish = fishService.getFishEntity(fishId);

    String imageUrl = s3Service.upload(image, PathName.FISH);
    try {
      // saveAndFlush: 제약 위반을 커밋 시점이 아니라 여기서 드러내야 아래 보상 삭제가 실제로 동작한다.
      CatchRecord saved =
          catchRecordRepository.saveAndFlush(
              CatchRecord.builder()
                  .userId(userId)
                  .fish(fish)
                  .certifiedImageUrl(imageUrl)
                  .size(size)
                  .catchLocation(catchLocation)
                  .build());
      // 이번 인증 포함 횟수 = 직전까지의 횟수 + 1. 저장된 컬럼이 아니라 행 개수에서 파생한다(옵션 B).
      int catchCount = (int) catchRecordRepository.countByUserIdAndFish_Id(userId, fishId);
      log.info(
          "어종 인증 저장: userId={}, fishId={}, size={}cm, location={}, {}번째",
          userId,
          fishId,
          size,
          catchLocation,
          catchCount);
      return VerifyResponse.of(saved, fish.getName(), catchCount);
    } catch (RuntimeException e) {
      // DB 저장이 실패하면 트랜잭션은 롤백되지만 S3 객체는 남는다 → 명시적으로 되돌린다(고아 객체 방지).
      log.error("인증 기록 저장 실패, 업로드한 사진을 되돌립니다: url={}, {}", imageUrl, e.getMessage());
      safeDelete(imageUrl);
      throw e;
    }
  }

  /**
   * 모델이 준 종명을 도감 어종으로 매핑한다.
   *
   * <p>종명 문자열이 두 시스템의 <b>조인 키</b>다. 도감에 없는 종명은 조용히 넘기지 않고 WARN 을 남긴 뒤 후보에서 제외한다 — 클라이언트가 선택할 수 없는
   * 후보를 보여주면 안 되기 때문이다. 현재 모델 24종과 도감 24종은 문자열까지 정확히 일치하므로 이 경로는 모델을 재학습해 클래스가 바뀐 경우에만 탄다.
   */
  private List<FishCandidateResponse> toCandidates(List<PredictionItem> predictions) {
    List<FishCandidateResponse> candidates = new ArrayList<>();
    for (PredictionItem item : predictions) {
      Optional<FishSummaryResponse> fish = findByName(item.species());
      if (fish.isEmpty()) {
        log.warn("모델 종명이 도감에 없습니다(조인 키 불일치): species={}", item.species());
        continue;
      }
      candidates.add(FishCandidateResponse.of(item.rank(), fish.get(), item.confidence()));
    }
    return candidates;
  }

  /** 어종명 완전일치 조회. fish 도메인의 기존 조회 메서드를 그대로 쓰며, 없으면 빈 값이다(예외 아님). */
  private Optional<FishSummaryResponse> findByName(String species) {
    if (species == null || species.isBlank()) {
      return Optional.empty();
    }
    return fishService.getFishList(species).fishes().stream().findFirst();
  }

  /** 보상 삭제는 best-effort 다 — 삭제까지 실패해도 원래 예외를 가리지 않는다. */
  private void safeDelete(String imageUrl) {
    try {
      s3Service.delete(imageUrl);
    } catch (RuntimeException e) {
      log.error("보상 삭제 실패(고아 객체 잔존): url={}, {}", imageUrl, e.getMessage());
    }
  }
}
