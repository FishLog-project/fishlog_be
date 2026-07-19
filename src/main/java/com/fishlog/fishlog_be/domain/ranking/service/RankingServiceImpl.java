package com.fishlog.fishlog_be.domain.ranking.service;

import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import com.fishlog.fishlog_be.domain.collection.repository.UserFishCount;
import com.fishlog.fishlog_be.domain.collection.repository.UserMaxSize;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.domain.ranking.dto.RankingEntryResponse;
import com.fishlog.fishlog_be.domain.ranking.dto.RankingResponse;
import com.fishlog.fishlog_be.domain.ranking.dto.RankingType;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 랭킹 집계·조립. 데이터 소유자인 {@link CatchRecordRepository}(점수)와 {@link FishRepository}(완성도 분모)를 재사용해 파생 집계만
 * 수행한다(랭킹 전용 테이블 없음). → docs/ranking.md
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

  private final CatchRecordRepository catchRecordRepository;
  private final FishRepository fishRepository;

  @Override
  public RankingResponse getCompletionRanking(Long userId) {
    long totalFishCount = fishRepository.countByIsCollectibleTrue();
    List<UserFishCount> rows = catchRecordRepository.findCompletionScores(); // 어종 수 내림차순

    // 동점 판정을 위해 점수(어종 수)만 뽑아 순위를 계산한다.
    List<Double> scores = rows.stream().map(r -> (double) r.getFishCount()).toList();
    int[] ranks = computeRanks(scores);

    List<RankingEntryResponse> rankings = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      UserFishCount row = rows.get(i);
      int caught = (int) row.getFishCount();
      rankings.add(
          RankingEntryResponse.completion(
              ranks[i], row.getUserId(), caught, completionRate(caught, totalFishCount)));
    }

    RankingEntryResponse me = findMe(rankings, userId);
    if (me == null && userId != null) {
      // 인증 기록이 없는 사용자: 순위 없음(rank=null), 완성도 0%.
      me = RankingEntryResponse.completion(null, userId, 0, 0.0);
    }

    return new RankingResponse(
        RankingType.COMPLETION, (int) totalFishCount, me, top3(rankings), rankings);
  }

  @Override
  public RankingResponse getSizeRanking(Long userId) {
    List<UserMaxSize> rows = catchRecordRepository.findMaxSizeScores(); // 최대 크기 내림차순

    List<Double> scores = rows.stream().map(UserMaxSize::getMaxSize).toList();
    int[] ranks = computeRanks(scores);

    List<RankingEntryResponse> rankings = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      UserMaxSize row = rows.get(i);
      rankings.add(RankingEntryResponse.size(ranks[i], row.getUserId(), row.getMaxSize()));
    }

    RankingEntryResponse me = findMe(rankings, userId);
    if (me == null && userId != null) {
      // 인증 기록이 없는 사용자: 순위 없음(rank=null), 최대 크기 없음(null).
      me = RankingEntryResponse.size(null, userId, null);
    }

    return new RankingResponse(RankingType.SIZE, null, me, top3(rankings), rankings);
  }

  /**
   * 점수 내림차순으로 정렬된 리스트에 공동 순위(1,1,3)를 부여한다.
   *
   * <p>규칙: 0번째는 1위. 이후 각 항목은 <b>직전 항목과 점수가 같으면 같은 rank</b>, 다르면 <b>rank = 인덱스 + 1</b>로 점프한다. 예) 점수
   * [93.1, 93.1, 86.2, 86.2, 69.0] → rank [1, 1, 3, 3, 5].
   *
   * @param sortedScoresDesc 내림차순 정렬된 점수(완성도=어종 수, 크기=최대 cm)
   * @return i번째 항목의 순위(1-based) 배열
   */
  private int[] computeRanks(List<Double> sortedScoresDesc) {
    int[] ranks = new int[sortedScoresDesc.size()];
    for (int i = 0; i < sortedScoresDesc.size(); i++) {
      if (i > 0 && sortedScoresDesc.get(i).doubleValue() == sortedScoresDesc.get(i - 1)) {
        ranks[i] = ranks[i - 1]; // 직전과 동점 → 같은 순위
      } else {
        ranks[i] = i + 1; // 새 점수 → 인덱스+1 (동점 수만큼 순위 점프)
      }
    }
    return ranks;
  }

  /** 전체 순위에서 본인(userId) 항목을 찾는다. userId가 없거나 기록이 없으면 null. */
  private RankingEntryResponse findMe(List<RankingEntryResponse> rankings, Long userId) {
    if (userId == null) {
      return null;
    }
    return rankings.stream().filter(e -> userId.equals(e.userId())).findFirst().orElse(null);
  }

  /** 상위 3명(전체 순위의 앞 3개). */
  private List<RankingEntryResponse> top3(List<RankingEntryResponse> rankings) {
    return rankings.stream().limit(3).toList();
  }

  /** 완성도 퍼센트(소수 1자리). 분모가 0이면 0%. */
  private double completionRate(int caught, long total) {
    if (total == 0) {
      return 0.0;
    }
    return Math.round(caught * 1000.0 / total) / 10.0;
  }
}
