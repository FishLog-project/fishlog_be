package com.fishlog.fishlog_be.domain.tour.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TourAPI 장소명 ↔ 집중률 관광지명 매칭 규칙.
 *
 * <p>아래 기대값은 전부 <b>2026-09-19 실호출에서 관측된 실제 표기</b>다. 임의로 만든 예가 아니라, 이 규칙이 현장에서 만나는 이름들이다.
 */
class CongestionMatcherTest {

  /** 부안군 집중률 데이터셋의 실제 이름들(일부). */
  private static final Map<String, Double> BUAN =
      Map.of(
          "채석강 (전북 서해안 국가지질공원)", 39.61,
          "적벽강 (전북 서해안 국가지질공원)", 41.2,
          "내소사(부안)", 23.63,
          "분옥담과 선녀탕", 11.45,
          "상록해수욕장", 55.0,
          "궁항", 30.0,
          "개암사", 26.31);

  @Nested
  @DisplayName("붙어야 하는 경우")
  class ShouldMatch {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("꼬리표·괄호·공백만 다른 같은 장소는 매칭된다")
    @CsvSource({
      "채석강, 39.61",
      "적벽강, 41.2",
      "내소사, 23.63",
      "분옥담과 선녀탕, 11.45",
      "상록해수욕장, 55.0",
      "개암사, 26.31",
    })
    void matchesDespiteNotation(String title, double expected) {
      assertThat(CongestionMatcher.match(title, BUAN))
          .isNotNull()
          .isCloseTo(expected, within(0.01));
    }

    @Test
    @DisplayName("이름이 완전히 같으면 당연히 매칭된다")
    void exactName() {
      assertThat(CongestionMatcher.match("채석강 (전북 서해안 국가지질공원)", BUAN))
          .isCloseTo(39.61, within(0.01));
    }
  }

  @Nested
  @DisplayName("붙으면 안 되는 경우")
  class ShouldNotMatch {

    @Test
    @DisplayName("데이터셋에 없는 장소는 null이다 — 항·포구는 대부분 여기 걸린다")
    void absentPlace() {
      assertThat(CongestionMatcher.match("격포항", BUAN)).isNull();
      assertThat(CongestionMatcher.match("부안 솔섬", BUAN)).isNull();
    }

    @Test
    @DisplayName("이름이 겹쳐도 다른 장소면 붙이지 않는다 — 틀린 혼잡도보다 빈칸이 낫다")
    void differentPlaceSharingName() {
      // '궁항'은 항구, '궁항리조트'는 그 앞 숙박시설이다. 혼잡도가 같을 이유가 없다.
      assertThat(CongestionMatcher.match("궁항리조트", BUAN)).isNull();
    }

    @Test
    @DisplayName("빈 입력·빈 데이터는 예외 없이 null이다")
    void emptyInputs() {
      assertThat(CongestionMatcher.match(null, BUAN)).isNull();
      assertThat(CongestionMatcher.match("  ", BUAN)).isNull();
      assertThat(CongestionMatcher.match("채석강", Map.of())).isNull();
      assertThat(CongestionMatcher.match("채석강", null)).isNull();
    }
  }

  @Nested
  @DisplayName("정규화")
  class Normalize {

    @Test
    @DisplayName("괄호 구간과 모든 공백을 지운다")
    void stripsParenthesesAndSpaces() {
      assertThat(CongestionMatcher.normalize("채석강 (전북 서해안 국가지질공원)")).isEqualTo("채석강");
      assertThat(CongestionMatcher.normalize("내소사(부안)")).isEqualTo("내소사");
      assertThat(CongestionMatcher.normalize("분옥담과 선녀탕")).isEqualTo("분옥담과선녀탕");
      assertThat(CongestionMatcher.normalize(null)).isEmpty();
    }
  }
}
