package com.fishlog.fishlog_be.domain.collection.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 분류 응답의 안내 문구 분기. 핵심 규칙은 "uncertain 이어도 후보를 숨기지 않는다"는 것이다. */
class ClassifyResponseTest {

  private static final List<FishCandidateResponse> CANDIDATES =
      List.of(
          new FishCandidateResponse(1, 15L, "붕어", null, 0.83),
          new FishCandidateResponse(2, 16L, "잉어", null, 0.05),
          new FishCandidateResponse(3, 20L, "가물치", null, 0.01));

  @Test
  @DisplayName("uncertain 이어도 후보 3개를 그대로 내려주고 재촬영 안내만 덧붙인다")
  void uncertainKeepsCandidates() {
    ClassifyResponse response = ClassifyResponse.of("b0-384-20260818", true, CANDIDATES);

    assertThat(response.uncertain()).isTrue();
    assertThat(response.candidates()).hasSize(3); // 후보를 숨기지 않는 것이 핵심
    assertThat(response.guide()).contains("다시 찍으면");
  }

  @Test
  @DisplayName("확신하는 경우엔 선택 안내를 준다")
  void confidentGuide() {
    ClassifyResponse response = ClassifyResponse.of("b0-384-20260818", false, CANDIDATES);

    assertThat(response.candidates()).hasSize(3);
    assertThat(response.guide()).contains("선택해주세요");
  }

  @Test
  @DisplayName("후보가 하나도 없으면 목록에서 직접 선택하도록 안내한다")
  void emptyCandidatesGuidesToManualPick() {
    ClassifyResponse response = ClassifyResponse.of("b0-384-20260818", false, List.of());

    assertThat(response.candidates()).isEmpty();
    assertThat(response.guide()).contains("직접 선택");
  }
}
