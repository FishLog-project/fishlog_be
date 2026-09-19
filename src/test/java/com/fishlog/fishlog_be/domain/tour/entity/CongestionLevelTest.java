package com.fishlog.fishlog_be.domain.tour.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 집중률 지수 → 혼잡도 등급 경계. */
class CongestionLevelTest {

  @ParameterizedTest(name = "지수 {0} → {1}")
  @DisplayName("경계값을 포함해 등급이 갈린다")
  @CsvSource({
    "0.0, 여유",
    "39.6, 여유",
    "49.99, 여유",
    "50.0, 보통",
    "69.7, 보통",
    "79.99, 보통",
    "80.0, 혼잡",
    "96.92, 혼잡",
    "100.0, 혼잡",
  })
  void boundaries(double rate, String expected) {
    assertThat(CongestionLevel.from(rate).label()).isEqualTo(expected);
  }

  @Test
  @DisplayName("라벨은 세 가지뿐이고 서로 다르다")
  void labels() {
    assertThat(CongestionLevel.values()).hasSize(3);
    assertThat(CongestionLevel.RELAXED.label()).isEqualTo("여유");
    assertThat(CongestionLevel.NORMAL.label()).isEqualTo("보통");
    assertThat(CongestionLevel.CROWDED.label()).isEqualTo("혼잡");
  }
}
