package com.fishlog.fishlog_be.domain.banner.dto;

import com.fishlog.fishlog_be.domain.spot.dto.PopularSpotResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 배너 추천 스팟 — 해양·내륙 각 카테고리에서 조회수 최다 스팟 1곳.
 *
 * @param marine 해양 스팟 중 조회수 최다(없으면 null)
 * @param inland 내륙 스팟 중 조회수 최다(없으면 null)
 */
@Schema(description = "배너 추천 스팟(해양·내륙 조회수 최다 각 1곳)")
public record RecommendedSpotsResponse(
    @Schema(description = "해양 조회수 최다 스팟(없으면 null)") PopularSpotResponse marine,
    @Schema(description = "내륙 조회수 최다 스팟(없으면 null)") PopularSpotResponse inland) {}
