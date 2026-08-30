package com.fishlog.fishlog_be.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.ai.dto.PredictResponse;
import com.fishlog.fishlog_be.global.ai.dto.PredictionItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 모델 서버 계약(docs/backend_handoff.md)을 코드가 실제로 지키는지 확인한다.
 *
 * <p>Spring 컨텍스트 없이 도는 순수 단위 테스트다 — 모델 서버는 사설 IP라 로컬에서 호출할 수 없으므로, 계약 문서의 <b>실제 응답 예시</b>를 그대로 넣어
 * 파싱·매핑을 검증하는 것이 로컬에서 할 수 있는 최선의 회귀 방어다.
 */
class PredictResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  @DisplayName("성공 응답의 snake_case 필드가 DTO에 매핑된다")
  void parseSuccessResponse() throws Exception {
    String json =
        """
        {
          "success": true,
          "uncertain": false,
          "model_version": "b0-384-20260818",
          "predictions": [
            {"rank": 1, "species": "붕어", "confidence": 0.83},
            {"rank": 2, "species": "잉어", "confidence": 0.05},
            {"rank": 3, "species": "가물치", "confidence": 0.01}
          ],
          "other_confidence": 0.01,
          "top1_confidence": 0.83,
          "latency_ms": 81.2
        }
        """;

    PredictResponse response = objectMapper.readValue(json, PredictResponse.class);

    assertThat(response.success()).isTrue();
    assertThat(response.uncertain()).isFalse();
    assertThat(response.modelVersion()).isEqualTo("b0-384-20260818");
    assertThat(response.otherConfidence()).isEqualTo(0.01);
    assertThat(response.top1Confidence()).isEqualTo(0.83);
    assertThat(response.latencyMs()).isEqualTo(81.2);
    assertThat(response.safePredictions())
        .extracting(PredictionItem::rank, PredictionItem::species, PredictionItem::confidence)
        .containsExactly(tuple(1, "붕어", 0.83), tuple(2, "잉어", 0.05), tuple(3, "가물치", 0.01));
  }

  @Test
  @DisplayName("실패 응답도 같은 DTO로 파싱돼 error 코드를 꺼낼 수 있다")
  void parseFailureResponse() throws Exception {
    String json =
        """
        {"success": false, "error": "IMAGE_DECODE_FAILED", "detail": "cannot identify image file"}
        """;

    PredictResponse response = objectMapper.readValue(json, PredictResponse.class);

    assertThat(response.success()).isFalse();
    assertThat(response.error()).isEqualTo("IMAGE_DECODE_FAILED");
    assertThat(response.safePredictions()).isEmpty(); // predictions 누락 → 널 대신 빈 리스트
  }

  @Test
  @DisplayName("모델이 필드를 추가해도 파싱이 깨지지 않는다")
  void ignoresUnknownFields() throws Exception {
    String json =
        """
        {"success": true, "uncertain": false, "predictions": [], "brand_new_field": 123}
        """;

    assertThat(objectMapper.readValue(json, PredictResponse.class).success()).isTrue();
  }

  @Test
  @DisplayName("모델 error 코드가 우리 에러 코드와 HTTP 상태로 매핑된다")
  void mapsModelErrorCodes() {
    assertThat(AiErrorCode.fromModelError("EMPTY_FILE")).isEqualTo(AiErrorCode.EMPTY_FILE);
    assertThat(AiErrorCode.fromModelError("IMAGE_DECODE_FAILED"))
        .isEqualTo(AiErrorCode.IMAGE_DECODE_FAILED);

    assertThat(AiErrorCode.fromModelError("UNSUPPORTED_FORMAT").getStatus())
        .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    assertThat(AiErrorCode.fromModelError("FILE_TOO_LARGE").getStatus())
        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(AiErrorCode.fromModelError("IMAGE_TOO_LARGE").getStatus())
        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(AiErrorCode.fromModelError("MODEL_NOT_LOADED").getStatus())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  @DisplayName("모르는 error 코드·null은 분류 불가로 떨어져 직접 선택 경로로 유도한다")
  void unknownErrorFallsBackToUnavailable() {
    assertThat(AiErrorCode.fromModelError("SOMETHING_NEW"))
        .isEqualTo(AiErrorCode.CLASSIFY_UNAVAILABLE);
    assertThat(AiErrorCode.fromModelError(null)).isEqualTo(AiErrorCode.CLASSIFY_UNAVAILABLE);
  }
}
