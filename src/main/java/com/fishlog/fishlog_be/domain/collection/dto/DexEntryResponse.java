package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 도감 그리드의 한 칸. 전체 도감 항목({@link FishSummaryResponse})에 "내가 잡았는지"({@code caught})를 덧입힌 형태다.
 *
 * <p>{@code imageUrl}은 <b>서버가 골라서</b> 내려준다 — {@code caught=true}면 도감 이미지({@code {영문어종명}_image}),
 * {@code false}면 그림자 이미지({@code {영문어종명}_shadow})다. 그림자를 클라이언트 이펙트로 만들지 않고 별도 파일로 준비했기 때문에, 어느 쪽을 쓸지
 * 아는 서버가 URL 하나로 확정해 주는 편이 화면 코드가 단순하다. {@code caught} 플래그는 "몇 종 잡았는지" 표시·필터 같은 다른 용도가 있어 그대로 유지한다.
 * → docs/media.md
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
    @Schema(
            description = "표시할 이미지 URL — caught=true면 어종 이미지, false면 그림자 이미지(파일 없으면 null)",
            example = "http://localhost:8080/images/fish/black_seabream_image.png")
        String imageUrl,
    @Schema(description = "희귀도", example = "USUALLY") Rarity rarity,
    @Schema(description = "서식지(바다/강/저수지/하천)", example = "바다") String habitat,
    @Schema(description = "내가 잡았는지 여부(imageUrl 이 이미지/그림자 중 무엇인지와 같은 의미)", example = "true")
        boolean caught) {

  /**
   * @param imageUrl 이 칸에 그릴 이미지 URL. 호출부(도감 서비스)가 {@code caught}에 따라 어종 이미지 또는 그림자 이미지를 골라 넘긴다 —
   *     {@code fish.imageUrl()}(항상 어종 이미지)을 그대로 쓰면 안 된다.
   */
  public static DexEntryResponse of(FishSummaryResponse fish, boolean caught, String imageUrl) {
    return new DexEntryResponse(
        fish.id(), fish.name(), imageUrl, fish.rarity(), fish.habitat(), caught);
  }
}
