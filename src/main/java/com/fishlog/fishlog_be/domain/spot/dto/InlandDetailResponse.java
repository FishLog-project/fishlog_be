package com.fishlog.fishlog_be.domain.spot.dto;

import com.fishlog.fishlog_be.domain.spot.entity.InlandSpotDetail;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내륙(담수) 스팟의 하천 제원. 해양 스팟의 {@code forecast}에 대응하는 자리이며, 예보와 달리 실측 저장값이라 항상 같은 값이 나온다.
 *
 * <p>조사에서 빠진 항목은 개별 {@code null}(하폭만 있고 유수폭·수심이 없는 스팟이 있다). → docs/spec.md
 */
@Schema(description = "내륙 스팟 하천 제원(하폭·유수폭·수심, 단위 m)")
public record InlandDetailResponse(
    @Schema(description = "최소 하폭(m)", example = "70.0") Double riverWidthMin,
    @Schema(description = "최대 하폭(m)", example = "83.0") Double riverWidthMax,
    @Schema(description = "최소 유수폭(m)", example = "15.0") Double flowWidthMin,
    @Schema(description = "최대 유수폭(m)", example = "60.0") Double flowWidthMax,
    @Schema(description = "최소 수심(m)", example = "0.2") Double depthMin,
    @Schema(description = "최대 수심(m)", example = "1.5") Double depthMax) {

  public static InlandDetailResponse from(InlandSpotDetail d) {
    return new InlandDetailResponse(
        d.getRiverWidthMin(),
        d.getRiverWidthMax(),
        d.getFlowWidthMin(),
        d.getFlowWidthMax(),
        d.getDepthMin(),
        d.getDepthMax());
  }
}
