package com.fishlog.fishlog_be.domain.ranking.dto;

/** 랭킹 기준. 두 기준은 화면·응답 구조가 같고 점수(metric)만 다르다. → docs/ranking.md */
public enum RankingType {
  /** 도감 완성도(고유 수집대상 어종 수 ÷ 전체 도감 어종 수). */
  COMPLETION,
  /** 잡은 어종 최대 크기(cm). */
  SIZE
}
