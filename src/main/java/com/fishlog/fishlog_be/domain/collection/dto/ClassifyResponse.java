package com.fishlog.fishlog_be.domain.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 어종 분류 결과(Top-3 후보). 저장은 하지 않는 <b>순수 조회</b> 응답이며, 사용자가 여기서 고른 {@code fishId}로 {@code POST
 * /api/collections/verify}를 호출해 도감에 기록한다.
 *
 * <p><b>{@code uncertain}이 true여도 후보는 그대로 내려준다.</b> 모델이 확신하지 못한다는 뜻일 뿐 후보가 틀렸다는 뜻은 아니므로, 후보를 숨기지 않고
 * {@code guide} 문구만 덧붙인다. Top-3 정확도는 90.7%다.
 *
 * <p>24종 밖 어종(향어·학꽁치 등)은 후보에 정답이 아예 없다 — 클라이언트는 항상 "목록에서 직접 선택" 대안 경로를 함께 제공해야 한다.
 */
@Schema(title = "ClassifyResponse DTO", description = "사진 기반 어종 분류 결과(Top-3 후보)")
public record ClassifyResponse(
    @Schema(description = "분류에 사용된 모델 버전", example = "b0-384-20260818") String modelVersion,
    @Schema(description = "모델이 확신하지 못하는 경우 true. 후보는 그대로 표시하되 재촬영을 권한다.", example = "false")
        boolean uncertain,
    @Schema(
            description = "사용자에게 보여줄 안내 문구",
            example = "후보 중에서 잡은 어종을 선택해주세요. 목록에 없으면 직접 선택할 수 있어요.")
        String guide,
    @Schema(description = "어종 후보 목록(신뢰도 내림차순, 최대 3건)") List<FishCandidateResponse> candidates) {

  private static final String GUIDE_CONFIDENT = "후보 중에서 잡은 어종을 선택해주세요. 목록에 없으면 직접 선택할 수 있어요.";
  private static final String GUIDE_UNCERTAIN =
      "사진이 흐리거나 어종이 잘 보이지 않아요. 다시 찍으면 더 정확해집니다. 후보가 맞다면 그대로 선택해도 괜찮아요.";
  private static final String GUIDE_EMPTY = "사진에서 어종을 찾지 못했어요. 목록에서 직접 선택해주세요.";

  public static ClassifyResponse of(
      String modelVersion, boolean uncertain, List<FishCandidateResponse> candidates) {
    String guide;
    if (candidates.isEmpty()) {
      guide = GUIDE_EMPTY;
    } else if (uncertain) {
      guide = GUIDE_UNCERTAIN;
    } else {
      guide = GUIDE_CONFIDENT;
    }
    return new ClassifyResponse(modelVersion, uncertain, guide, candidates);
  }
}
