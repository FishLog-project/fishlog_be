package com.fishlog.fishlog_be.domain.fish.dto;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import io.swagger.v3.oas.annotations.media.Schema;

/** 전체 도감 목록 항목(그리드/썸네일용 요약). */
@Schema(title = "FishSummaryResponse DTO", description = "전체 도감 목록 항목")
public record FishSummaryResponse(
    @Schema(description = "어종 ID", example = "1") Long id,
    @Schema(description = "어종명", example = "감성돔") String name,
    @Schema(
            description = "도감 이미지 URL(서버 정적 파일). 파일이 아직 없으면 null",
            example = "http://localhost:8080/images/fish/black_seabream_image.png")
        String imageUrl,
    @Schema(description = "희귀도", example = "USUALLY") Rarity rarity,
    @Schema(description = "서식지(바다/강/저수지/하천)", example = "바다") String habitat) {

  /**
   * @param imageUrl 도감 이미지 URL. 엔티티 컬럼이 아니라 {@link
   *     com.fishlog.fishlog_be.global.image.FishImageService}가 어종명으로 만들어 준 값을 받는다(호출부 = fish 서비스).
   *     → docs/media.md
   */
  public static FishSummaryResponse of(Fish fish, String imageUrl) {
    return new FishSummaryResponse(
        fish.getId(), fish.getName(), imageUrl, fish.getRarity(), fish.getHabitat());
  }
}
