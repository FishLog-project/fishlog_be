package com.fishlog.fishlog_be.domain.fish.entity;

/**
 * 제철(계절). 월→계절 매핑과 어종의 계절 플래그 판정을 담는다.
 *
 * <p>월 경계: 봄 3~5월 / 여름 6~8월 / 가을 9~11월 / 겨울 12~2월. 배너 "계절별 추천 어종"이 현재 월로 계절을 정하는 데 쓴다. →
 * docs/spec.md
 */
public enum Season {
  SPRING,
  SUMMER,
  FALL,
  WINTER;

  /**
   * 월(1~12)을 계절로 매핑한다.
   *
   * @throws IllegalArgumentException 월이 1~12 범위를 벗어나면
   */
  public static Season of(int month) {
    return switch (month) {
      case 3, 4, 5 -> SPRING;
      case 6, 7, 8 -> SUMMER;
      case 9, 10, 11 -> FALL;
      case 12, 1, 2 -> WINTER;
      default -> throw new IllegalArgumentException("month must be 1~12: " + month);
    };
  }

  /** 어종이 이 계절에 제철인지 판정한다(계절 boolean 플래그 조회). */
  public boolean matches(Fish fish) {
    return switch (this) {
      case SPRING -> fish.isSpringSeason();
      case SUMMER -> fish.isSummerSeason();
      case FALL -> fish.isFallSeason();
      case WINTER -> fish.isWinterSeason();
    };
  }
}
