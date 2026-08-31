package com.fishlog.fishlog_be.domain.spot.dto;

import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/** 낚시 스팟(지도 마커) 응답 — DB 불변 정보만. → docs/spec.md, docs/geo.md */
@Schema(description = "낚시 스팟(지도 마커) 응답")
public record SpotResponse(
    @Schema(description = "스팟 ID", example = "1") Long id,
    @Schema(description = "위치명", example = "가거도") String name,
    @Schema(description = "위도", example = "34.07308") double lat,
    @Schema(description = "경도(longitude)", example = "125.08805") double lot,
    @Schema(description = "분류(해양/내륙)", example = "해양") SpotCategory category,
    @Schema(description = "로그인 사용자의 찜 여부", example = "false") boolean isFavorite) {

  public static SpotResponse of(Spot spot, boolean isFavorite) {
    return new SpotResponse(
        spot.getId(), spot.getName(), spot.getLat(), spot.getLot(), spot.getCategory(), isFavorite);
  }
}
