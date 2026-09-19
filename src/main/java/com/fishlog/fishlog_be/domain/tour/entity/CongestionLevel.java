package com.fishlog.fishlog_be.domain.tour.entity;

/**
 * 당일 혼잡도 등급. 집중률 지수(0~100)를 사용자가 읽을 수 있는 라벨로 바꾼다.
 *
 * <p><b>3등분(33/66)이 아닌 이유:</b> {@code cnctrRate}는 방문자 비율이 아니라 <b>2018년 이후 피크를 100으로 정규화한 지수</b>라
 * 분포가 위쪽으로 쏠려 있다. 실측(2026-09-19, 통영·속초·제주시·태안 485건)에서 중앙값이 69.7, 70 초과가 49%였다. 33/66으로 자르면 거의 모든
 * 관광지가 "혼잡"으로 찍혀 라벨이 정보를 잃는다. 아래 경계는 그 분포에서 대략 여유 24% / 보통 51% / 혼잡 25%가 되도록 잡았다.
 */
public enum CongestionLevel {
  RELAXED("여유"),
  NORMAL("보통"),
  CROWDED("혼잡");

  /** 여유 ↔ 보통 경계. 미만이면 여유. */
  private static final double NORMAL_THRESHOLD = 50.0;

  /** 보통 ↔ 혼잡 경계. 이상이면 혼잡. */
  private static final double CROWDED_THRESHOLD = 80.0;

  private final String label;

  CongestionLevel(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** 집중률 지수를 등급으로 변환한다. */
  public static CongestionLevel from(double rate) {
    if (rate < NORMAL_THRESHOLD) {
      return RELAXED;
    }
    if (rate < CROWDED_THRESHOLD) {
      return NORMAL;
    }
    return CROWDED;
  }
}
