package com.fishlog.fishlog_be.domain.spot.dto;

import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 인기 스팟(조회수 상위) 응답 항목. */
@Schema(description = "인기 스팟(조회수 상위) 항목")
public record PopularSpotResponse(
    @Schema(description = "스팟 ID", example = "1") Long id,
    @Schema(description = "위치명", example = "가거도") String name,
    @Schema(description = "위도", example = "34.07308") double lat,
    @Schema(description = "경도(longitude)", example = "125.08805") double lot,
    @Schema(description = "분류(해양/내륙)", example = "해양") SpotCategory category,
    @Schema(description = "누적 조회수", example = "128") long viewCount,
    @Schema(description = "주요 대상 어종명 목록", example = "[\"감성돔\", \"참돔\"]") List<String> majorFishes) {

  public static PopularSpotResponse of(Spot spot, List<String> majorFishes) {
    return new PopularSpotResponse(
        spot.getId(),
        spot.getName(),
        spot.getLat(),
        spot.getLot(),
        spot.getCategory(),
        spot.getViewCount(),
        majorFishes);
  }
}
