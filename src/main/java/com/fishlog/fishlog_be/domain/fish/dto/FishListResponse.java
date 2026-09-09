package com.fishlog.fishlog_be.domain.fish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 전체 도감 목록 응답 래퍼. 수집 대상 어종 총 수와 목록을 함께 담는다. */
@Schema(title = "FishListResponse DTO", description = "전체 도감 목록(총 수 + 어종 목록)")
public record FishListResponse(
    @Schema(description = "수집 대상 어종 총 수", example = "6") int totalCount,
    @Schema(description = "어종 목록") List<FishSummaryResponse> fishes) {

  /**
   * @param fishes 이미 이미지 URL 까지 채워진 항목들. 엔티티가 아니라 DTO 를 받는 이유는 이미지 URL 이 엔티티 컬럼이 아니라 어종명으로 만드는 값이라,
   *     변환에 {@link com.fishlog.fishlog_be.global.image.FishImageService}가 필요하기 때문이다(그 의존은 서비스가
   *     가진다).
   */
  public static FishListResponse of(List<FishSummaryResponse> fishes) {
    return new FishListResponse(fishes.size(), fishes);
  }
}
