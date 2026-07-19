package com.fishlog.fishlog_be.domain.ranking.service;

import com.fishlog.fishlog_be.domain.ranking.dto.RankingResponse;

/** 사용자 랭킹 조회. 두 기준(완성도·크기)을 각각 제공한다. → docs/ranking.md */
public interface RankingService {

  /**
   * 도감 완성도 랭킹.
   *
   * @param userId 본인 순위(me) 계산용 임시 파라미터. null이면 me는 null.
   */
  RankingResponse getCompletionRanking(Long userId);

  /**
   * 최대 어종 크기 랭킹.
   *
   * @param userId 본인 순위(me) 계산용 임시 파라미터. null이면 me는 null.
   */
  RankingResponse getSizeRanking(Long userId);
}
