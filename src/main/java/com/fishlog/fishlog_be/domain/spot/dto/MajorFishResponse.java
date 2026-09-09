package com.fishlog.fishlog_be.domain.spot.dto;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 스팟의 주요 대상 어종 1건. 도감({@code fishes})에 있는 어종만 매핑되므로 도감 정보(이미지 등)를 그대로 노출한다.
 *
 * <p>{@code imageUrl}은 {@code fishes.image_url} 컬럼이 아니라 어종명 기반으로 {@code FishImageService}가 생성한
 * 값이다(도감 조회·상세와 동일 방식). → docs/media.md
 *
 * @param fishId 어종 id(도감 상세 {@code GET /api/fish/{id}} 연결용)
 * @param name 어종명
 * @param imageUrl 도감 이미지 URL(파일 없거나 매핑 전이면 null)
 */
@Schema(description = "스팟 주요 대상 어종")
public record MajorFishResponse(
    @Schema(description = "어종 id", example = "1") Long fishId,
    @Schema(description = "어종명", example = "감성돔") String name,
    @Schema(description = "도감 이미지 URL(없으면 null)") String imageUrl) {

  /** 어종 + 이미지 URL(호출부에서 {@code FishImageService}로 생성)로 응답을 만든다. */
  public static MajorFishResponse of(Fish fish, String imageUrl) {
    return new MajorFishResponse(fish.getId(), fish.getName(), imageUrl);
  }
}
