package com.fishlog.fishlog_be.domain.fish.dto;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import io.swagger.v3.oas.annotations.media.Schema;

/** 전체 도감 목록 항목(그리드/썸네일용 요약). */
@Schema(title = "FishSummaryResponse DTO", description = "전체 도감 목록 항목")
public record FishSummaryResponse(
    @Schema(description = "어종 ID", example = "1") Long id,
    @Schema(description = "어종명", example = "감성돔") String name,
    @Schema(description = "도감 이미지 URL(S3)", example = "https://.../fish/1.png") String imageUrl,
    @Schema(description = "희귀도", example = "USUALLY") Rarity rarity) {

  public static FishSummaryResponse from(Fish fish) {
    return new FishSummaryResponse(
        fish.getId(), fish.getName(), fish.getImageUrl(), fish.getRarity());
  }
}
