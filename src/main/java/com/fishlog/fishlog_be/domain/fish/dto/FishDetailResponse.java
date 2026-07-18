package com.fishlog.fishlog_be.domain.fish.dto;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import io.swagger.v3.oas.annotations.media.Schema;

/** 어종 상세 응답(도감 상세 화면). */
@Schema(title = "FishDetailResponse DTO", description = "어종 상세")
public record FishDetailResponse(
    @Schema(description = "어종 ID", example = "1") Long id,
    @Schema(description = "어종명", example = "감성돔") String name,
    @Schema(description = "어종 설명", example = "연안 방파제에서 흔히 잡히는 돔.") String description,
    @Schema(description = "서식지", example = "남해 연안") String habitat,
    @Schema(description = "도감 이미지 URL(S3)", example = "https://.../fish/1.png") String imageUrl,
    @Schema(description = "희귀도", example = "USUALLY") Rarity rarity) {

  public static FishDetailResponse from(Fish fish) {
    return new FishDetailResponse(
        fish.getId(),
        fish.getName(),
        fish.getDescription(),
        fish.getHabitat(),
        fish.getImageUrl(),
        fish.getRarity());
  }
}
