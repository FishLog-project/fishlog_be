package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.DexEntryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionServiceImpl implements CollectionService {

  private final CatchRecordRepository catchRecordRepository;
  // 도메인 간 접근은 상대 도메인의 service 인터페이스로만 한다(fish 의 repository·entity 직접 접근 금지).
  private final FishService fishService;

  @Override
  public CatchRecordResponse getMyCatch(Long userId, Long fishId) {
    // (userId, fishId) 로 인증 기록을 모아 응답으로 변환한다. 안 잡았으면 빈 리스트 → 200 + catchCount 0.
    List<CatchRecord> records = catchRecordRepository.findByUserIdAndFish_Id(userId, fishId);
    return CatchRecordResponse.of(records);
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
}
