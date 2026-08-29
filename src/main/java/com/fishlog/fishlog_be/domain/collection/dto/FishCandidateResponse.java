package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 어종 분류 후보 1건. 모델이 준 종명을 도감 어종({@code fishes})에 매핑해 {@code fishId}까지 채운 형태다.
 *
 * <p><b>여기 담긴 후보는 모두 선택 가능하다</b> — 도감에 없는 종명은 서비스가 WARN 로그와 함께 제외하므로 클라이언트는 {@code fishId}가 null인
 * 경우를 다루지 않아도 된다.
 */
@Schema(title = "FishCandidateResponse DTO", description = "어종 분류 후보(Top-3 중 1건)")
public record FishCandidateResponse(
    @Schema(description = "후보 순위(1이 가장 유력)", example = "1") int rank,
    @Schema(description = "어종 ID — 인증 요청의 fishId로 그대로 사용", example = "15") Long fishId,
    @Schema(description = "어종명", example = "붕어") String name,
    @Schema(description = "도감 이미지 URL(S3)", example = "https://.../fish/15.png") String imageUrl,
    @Schema(
            description = "모델 신뢰도(0~1). 25클래스 softmax 원값이라 후보들의 합이 1이 아니며, 보정 전이라 과신 경향이 있다.",
            example = "0.83")
        double confidence) {

  public static FishCandidateResponse of(int rank, FishSummaryResponse fish, double confidence) {
    return new FishCandidateResponse(rank, fish.id(), fish.name(), fish.imageUrl(), confidence);
  }
}
