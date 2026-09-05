package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CustomCatchDetailResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CustomCatchResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CustomDexEntryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyCustomDexResponse;
import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import com.fishlog.fishlog_be.domain.collection.exception.CollectionErrorCode;
import com.fishlog.fishlog_be.domain.collection.policy.CatchRecordPolicy;
import com.fishlog.fishlog_be.domain.collection.repository.CatchStats;
import com.fishlog.fishlog_be.domain.collection.repository.CustomCatchRecordRepository;
import com.fishlog.fishlog_be.domain.collection.repository.CustomFishRepository;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.s3.PathName;
import com.fishlog.fishlog_be.global.s3.S3Service;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class CustomCatchServiceImpl implements CustomCatchService {

  private final CustomFishRepository customFishRepository;
  private final CustomCatchRecordRepository customCatchRecordRepository;
  // 도감에 이미 있는 이름인지 확인하는 용도로만 쓴다(도메인 간 접근은 service 인터페이스로).
  private final FishService fishService;
  private final S3Service s3Service;

  @Override
  @Transactional
  public CustomCatchResponse register(
      Long userId,
      String fishName,
      String habitat,
      Double size,
      String location,
      MultipartFile image) {
    // 검증·정규화를 업로드보다 먼저 끝낸다 — 400으로 끊을 요청이라면 S3에 올리기 전이어야 한다.
    String name = CatchRecordPolicy.normalizeFishName(fishName);
    String fishHabitat = CatchRecordPolicy.normalizeHabitat(habitat);
    CatchRecordPolicy.validateSize(size);
    String catchLocation = CatchRecordPolicy.normalizeLocation(location);
    rejectIfInDex(name);

    CustomFish customFish = findOrCreateFish(userId, name, fishHabitat);

    String imageUrl = s3Service.upload(image, PathName.CUSTOM_FISH);
    try {
      // saveAndFlush: 제약 위반을 커밋 시점이 아니라 여기서 드러내야 아래 보상 삭제가 실제로 동작한다.
      CustomCatchRecord saved =
          customCatchRecordRepository.saveAndFlush(
              CustomCatchRecord.builder()
                  .userId(userId)
                  .customFish(customFish)
                  .certifiedImageUrl(imageUrl)
                  .size(size)
                  .catchLocation(catchLocation)
                  .build());
      log.info(
          "도감 외 어종 등록: userId={}, customFishId={}, name={}, habitat={}, size={}cm, location={}",
          userId,
          customFish.getId(),
          name,
          customFish.getHabitat(),
          size,
          catchLocation);
      return CustomCatchResponse.from(saved);
    } catch (RuntimeException e) {
      // DB 저장이 실패하면 트랜잭션은 롤백되지만 S3 객체는 남는다 → 명시적으로 되돌린다(고아 객체 방지).
      log.error("도감 외 어종 기록 저장 실패, 업로드한 사진을 되돌립니다: url={}, {}", imageUrl, e.getMessage());
      safeDelete(imageUrl);
      throw e;
    }
  }

  @Override
  public MyCustomDexResponse getMyCustomDex(Long userId) {
    // 최신순 전체를 어종과 함께 한 번에 받아 메모리에서 묶는다(쿼리 1회). 정렬은 리포지토리가 보장한다.
    List<CustomCatchRecord> records = customCatchRecordRepository.findAllWithFishByUserId(userId);

    // LinkedHashMap: 삽입 순서 = 최신순 목록에서 그 어종이 처음 나온 순서 = "가장 최근에 잡은 어종"부터.
    // 그룹 내부도 최신순이 유지되므로 맨 앞이 대표 이미지가 된다.
    Map<Long, List<CustomCatchRecord>> grouped = new LinkedHashMap<>();
    Map<Long, CustomFish> fishes = new LinkedHashMap<>();
    for (CustomCatchRecord record : records) {
      CustomFish fish = record.getCustomFish();
      fishes.putIfAbsent(fish.getId(), fish);
      grouped.computeIfAbsent(fish.getId(), id -> new ArrayList<>()).add(record);
    }

    List<CustomDexEntryResponse> entries =
        grouped.entrySet().stream()
            .map(entry -> CustomDexEntryResponse.of(fishes.get(entry.getKey()), entry.getValue()))
            .toList();
    return MyCustomDexResponse.of(entries);
  }

  @Override
  public CustomCatchDetailResponse getMyCustomCatch(Long userId, Long customFishId) {
    // 소유자 조건을 쿼리에 넣어 "남의 어종"과 "없는 어종"을 같은 404 로 수렴시킨다(존재 여부도 숨긴다).
    CustomFish fish =
        customFishRepository
            .findByIdAndUserId(customFishId, userId)
            .orElseThrow(() -> new CustomException(CollectionErrorCode.CUSTOM_FISH_NOT_FOUND));
    // 횟수·최대 크기는 사진 4장으로는 계산할 수 없어 따로 집계한다(도감 상세와 같은 이유·같은 프로젝션).
    CatchStats stats = customCatchRecordRepository.findStatsByCustomFishId(customFishId);
    List<CustomCatchRecord> recentRecords =
        customCatchRecordRepository.findByCustomFish_IdOrderByCreatedAtDescIdDesc(
            customFishId, PageRequest.of(0, CatchRecordPolicy.RECENT_PHOTO_LIMIT));
    return CustomCatchDetailResponse.of(
        fish, (int) stats.getCatchCount(), stats.getMaxSize(), recentRecords);
  }

  @Override
  @Transactional
  public void deleteMyRecords(Long userId) {
    // 기록 → 어종 순서를 지킨다. 뒤집으면 custom_fish_id FK 제약에 걸린다.
    customCatchRecordRepository.deleteByUserId(userId);
    customFishRepository.deleteByUserId(userId);
  }

  /**
   * 이 사용자의 어종 목록에서 같은 이름을 찾고, 없으면 만든다.
   *
   * <p>같은 이름으로 다시 등록할 때 어종 행이 늘지 않아야 "같은 이름 = 같은 어종"이 성립한다. 이름이 이미 있으면 서식지만 이번 입력으로 갱신한다 — 처음엔 비워
   * 뒀다가 나중에 채우는 흐름이 자연스럽고, 그게 사용자의 최신 의도다.
   *
   * <p>{@code UNIQUE(user_id, name)}가 최종 방어선이다. 같은 이름을 동시에 두 번 등록하면 이 조회가 둘 다 빈 결과를 받을 수 있는데, 그때는
   * 제약 위반으로 한쪽 트랜잭션이 실패해 <b>중복 어종이 만들어지는 대신 요청 하나가 실패</b>한다(사용자는 재시도하면 된다).
   */
  private CustomFish findOrCreateFish(Long userId, String name, String habitat) {
    return customFishRepository
        .findByUserIdAndName(userId, name)
        .map(
            fish -> {
              fish.updateHabitatIfPresent(habitat);
              return fish;
            })
        .orElseGet(
            () ->
                customFishRepository.save(
                    CustomFish.builder().userId(userId).name(name).habitat(habitat).build()));
  }

  /**
   * 도감에 이미 있는 어종명이면 거부한다.
   *
   * <p>저장에 성공시키는 편이 사용자에게 친절해 보이지만 실제로는 반대다 — 그렇게 등록된 "붕어"는 도감 칸을 채우지 않고 크기 랭킹에도 잡히지 않는데, 사용자는 분명
   * 붕어를 등록했으므로 이유를 알 수 없는 상태가 된다. 여기서 400으로 끊고 도감 인증({@code /verify})으로 돌려보내는 편이 결과가 낫다.
   *
   * <p>판정은 <b>완전일치</b>다({@code FishService.getFishList(name)}). "참붕어"처럼 다른 이름은 그대로 통과시킨다 — 부분일치로
   * 넓히면 실제로 도감 밖 어종인 이름까지 막혀 등록 자체가 불가능해진다.
   */
  private void rejectIfInDex(String fishName) {
    boolean inDex = !fishService.getFishList(fishName).fishes().isEmpty();
    if (inDex) {
      log.info("도감에 있는 어종명으로 기타 어종 등록 시도: fishName={}", fishName);
      throw new CustomException(CollectionErrorCode.FISH_ALREADY_IN_DEX);
    }
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
