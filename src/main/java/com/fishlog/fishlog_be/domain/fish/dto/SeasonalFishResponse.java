package com.fishlog.fishlog_be.domain.fish.dto;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계절별 추천 어종 1건 — 배너용 최소 정보(어종명 + 이미지).
 *
 * @param fishId 어종 id
 * @param name 어종명
 * @param imageUrl 도감 이미지 URL. 아직 큐레이션 전이라 현재는 null 일 수 있다.
 */
@Schema(description = "계절별 추천 어종(배너)")
public record SeasonalFishResponse(
    @Schema(description = "어종 id", example = "9") Long fishId,
    @Schema(description = "어종명", example = "갈치") String name,
    @Schema(description = "도감 이미지 URL(미큐레이션 시 null)", example = "https://.../galchi.jpg")
        String imageUrl) {

  public static SeasonalFishResponse from(Fish fish) {
    return new SeasonalFishResponse(fish.getId(), fish.getName(), fish.getImageUrl());
  }
}
