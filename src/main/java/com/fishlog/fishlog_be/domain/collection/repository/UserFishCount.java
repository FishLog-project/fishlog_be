package com.fishlog.fishlog_be.domain.collection.repository;

/**
 * 사용자별 "고유 수집대상 어종 수" 집계 프로젝션(완성도 랭킹 분자).
 *
 * <p>옵션 B에서 같은 어종을 여러 번 인증하면 여러 행이 되므로, 완성도는 {@code COUNT(DISTINCT fishes_id)}로 센다. 랭킹 도메인이 재사용한다.
 * → docs/ranking.md
 */
public interface UserFishCount {
  Long getUserId();

  long getFishCount();
}
