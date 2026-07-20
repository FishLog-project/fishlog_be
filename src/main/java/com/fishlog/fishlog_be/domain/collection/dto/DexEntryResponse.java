package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 도감 그리드의 한 칸. 전체 도감 항목({@link FishSummaryResponse})에 "내가 잡았는지"({@code caught})를 덧입힌 형태다.
 *
 * <p>UI는 {@code caught=true}면 도감 이미지({@code imageUrl})를, {@code false}면 같은 이미지를 그림자(실루엣)로 렌더한다.
 * 그림자는 클라이언트 이펙트라 서버는 플래그만 내려준다.
 */
@Schema(title = "DexEntryResponse DTO", description = "내 도감 항목(어종 + 잡음 여부)")
public record DexEntryResponse(
    @Schema(description = "어종 ID", example = "1") Long id,
    @Schema(description = "어종명", example = "감성돔") String name,
    @Schema(description = "도감 이미지 URL(S3)", example = "https://.../fish/1.png") String imageUrl,
    @Schema(description = "희귀도", example = "USUALLY") Rarity rarity,
    @Schema(description = "내가 잡았는지 여부(true=이미지, false=그림자)", example = "true") boolean caught) {

  public static DexEntryResponse of(FishSummaryResponse fish, boolean caught) {
    return new DexEntryResponse(fish.id(), fish.name(), fish.imageUrl(), fish.rarity(), caught);
  }
}
