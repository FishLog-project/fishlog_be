package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 도감 그리드의 한 칸. 전체 도감 항목({@link FishSummaryResponse})에 "내가 잡았는지"({@code caught})를 덧입힌 형태다.
 *
 * <p>UI는 {@code caught=true}면 도감 이미지({@code imageUrl})를, {@code false}면 같은 이미지를 그림자(실루엣)로 렌더한다.
 * 그림자는 클라이언트 이펙트라 서버는 플래그만 내려준다.
 *
 * <p>{@code habitat}(바다/강/저수지/하천)은 도감을 서식지별로 묶어 보여주기 위한 값이다. 콘텐츠 시드가 채우지 않은 어종은 {@code null}일 수
 * 있으므로 클라이언트는 "기타" 등으로 처리해야 한다.
 *
 * <p>잡은 <b>횟수</b>는 여기에 담지 않는다 — 그리드는 획득/미획득만 그리고, 횟수·사진은 칸을 눌렀을 때 {@code GET
 * /api/collections?fishId=}로 따로 조회한다. 그리드가 안 쓰는 값을 24칸 전부에 실어 보내지 않기 위함이다.
 */
@Schema(title = "DexEntryResponse DTO", description = "내 도감 항목(어종 + 잡음 여부)")
public record DexEntryResponse(
    @Schema(description = "어종 ID", example = "1") Long id,
    @Schema(description = "어종명", example = "감성돔") String name,
    @Schema(description = "도감 이미지 URL(S3)", example = "https://.../fish/1.png") String imageUrl,
    @Schema(description = "희귀도", example = "USUALLY") Rarity rarity,
    @Schema(description = "서식지(바다/강/저수지/하천)", example = "바다") String habitat,
    @Schema(description = "내가 잡았는지 여부(true=이미지, false=그림자)", example = "true") boolean caught) {

  public static DexEntryResponse of(FishSummaryResponse fish, boolean caught) {
    return new DexEntryResponse(
        fish.id(), fish.name(), fish.imageUrl(), fish.rarity(), fish.habitat(), caught);
  }
}
