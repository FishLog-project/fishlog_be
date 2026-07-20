package com.fishlog.fishlog_be.domain.collection.repository;

/**
 * 사용자별 "잡은 어종 최대 크기(cm)" 집계 프로젝션(크기 랭킹 점수).
 *
 * <p>{@code catch_record.size}(NOT NULL Double)의 사용자별 {@code MAX}. 랭킹 도메인이 재사용한다. → docs/ranking.md
 */
public interface UserMaxSize {
  Long getUserId();

  Double getMaxSize();
}
