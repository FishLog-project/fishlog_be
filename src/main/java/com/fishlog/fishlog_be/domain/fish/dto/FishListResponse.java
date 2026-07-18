package com.fishlog.fishlog_be.domain.fish.dto;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 전체 도감 목록 응답 래퍼. 수집 대상 어종 총 수와 목록을 함께 담는다. */
@Schema(title = "FishListResponse DTO", description = "전체 도감 목록(총 수 + 어종 목록)")
public record FishListResponse(
    @Schema(description = "수집 대상 어종 총 수", example = "6") int totalCount,
    @Schema(description = "어종 목록") List<FishSummaryResponse> fishes) {

  public static FishListResponse of(List<Fish> fishList) {
    List<FishSummaryResponse> fishes = fishList.stream().map(FishSummaryResponse::from).toList();
    return new FishListResponse(fishes.size(), fishes);
  }
}
