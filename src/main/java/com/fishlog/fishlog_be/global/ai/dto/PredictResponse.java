package com.fishlog.fishlog_be.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 모델 서버 {@code POST /predict} 응답. 성공/실패 두 형태를 한 record 로 받는다(성공이면 {@code predictions}, 실패면 {@code
 * error}·{@code detail}이 채워진다). → docs/external.md §2
 *
 * <p>모델 서버가 필드를 추가해도 깨지지 않도록 {@code ignoreUnknown = true}를 둔다. snake_case 필드는
 * {@code @JsonProperty}로 매핑한다.
 *
 * <p>{@code 기타}(비물고기·24종 밖) 클래스는 모델 서버가 이미 후보에서 제외하고 {@code uncertain} 판정까지 끝내서 준다. 백엔드는 임계값을 다시
 * 계산하지 않는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictResponse(
    boolean success,
    boolean uncertain,
    @JsonProperty("model_version") String modelVersion,
    List<PredictionItem> predictions,
    @JsonProperty("other_confidence") Double otherConfidence,
    @JsonProperty("top1_confidence") Double top1Confidence,
    @JsonProperty("latency_ms") Double latencyMs,
    // 실패 응답 전용: {"success": false, "error": "<코드>", "detail": "..."}
    String error,
    String detail) {

  /** 후보 목록(널 안전). 실패 응답이면 빈 리스트. */
  public List<PredictionItem> safePredictions() {
    return predictions == null ? List.of() : predictions;
  }
}
