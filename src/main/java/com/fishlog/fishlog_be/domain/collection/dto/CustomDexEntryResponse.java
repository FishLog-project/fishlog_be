package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import java.util.List;

/**
 * 내 도감 외 어종 그리드의 한 칸. {@link DexEntryResponse}(도감 그리드)와 같은 자리의 DTO다.
 *
 * <p><b>도감 그리드와 다른 점 3가지</b>(같은 스펙을 목표로 하되, 값이 없거나 의미가 없는 필드는 싣지 않는다):
 *
 * <ul>
 *   <li>{@code caught} 없음 — 도감은 24칸 중 안 잡은 칸을 그림자로 그리지만, 이 목록은 <b>등록해야 생기는 칸</b>이라 전부 {@code
 *       true}다. 항상 같은 값인 필드는 화면 분기에 쓸 수 없다.
 *   <li>{@code rarity} 없음 — 희귀도는 도감 마스터 데이터의 속성이고, 사용자가 만든 어종에는 정할 주체가 없다.
 *   <li>{@code catchCount}·{@code maxSize} 있음 — 도감 그리드는 이 둘을 일부러 뺐지만(24칸이 안 쓰는 값이라), 이 목록은 "같은 이름을
 *       몇 번 잡았는지"를 칸에 바로 보여 주는 것이 요구사항이다.
 * </ul>
 *
 * <p>{@code imageUrl}은 도감처럼 고정 이미지가 없어 <b>가장 최근에 등록한 사진</b>을 쓴다 — 사용자가 만든 어종의 대표 이미지로 그만한 후보가 없고, 새
 * 사진을 올리면 칸이 자연스럽게 갱신된다.
 */
@Schema(title = "CustomDexEntryResponse DTO", description = "내 도감 외 어종 항목(어종 + 잡은 횟수·최대 크기)")
public record CustomDexEntryResponse(
    @Schema(description = "도감 외 어종 ID(상세 조회 시 customFishId 로 사용)", example = "3") Long id,
    @Schema(description = "사용자가 수기 입력한 어종명", example = "쏘가리") String name,
    @Schema(
            description = "대표 이미지 URL — 가장 최근에 등록한 사진",
            example = "https://.../custom-fish/uuid1.jpg")
        String imageUrl,
    @Schema(
            description = "주요 서식지(수기 입력, 미입력이면 null)",
            example = "강",
            requiredMode = RequiredMode.NOT_REQUIRED)
        String habitat,
    @Schema(description = "이 어종을 등록한 총 횟수", example = "3") int catchCount,
    @Schema(description = "이 어종으로 등록한 것 중 가장 큰 크기(cm)", example = "41.0") Double maxSize) {

  /**
   * 한 어종의 기록들로부터 그리드 한 칸을 만든다.
   *
   * @param fish 그룹 기준이 된 사용자별 어종(이름·서식지의 출처)
   * @param records 그 어종의 기록 전체. <b>최신순으로 정렬돼 있어야 한다</b> — 대표 이미지로 맨 앞(=가장 최근) 사진을 쓴다
   */
  public static CustomDexEntryResponse of(CustomFish fish, List<CustomCatchRecord> records) {
    // 최대 크기는 전체 기록에서 구한다. size 는 NOT NULL 이고 어종 행은 기록이 있어야 생기므로 항상 값이 있다.
    Double maxSize =
        records.stream().map(CustomCatchRecord::getSize).max(Double::compare).orElse(null);
    return new CustomDexEntryResponse(
        fish.getId(),
        fish.getName(),
        records.getFirst().getCertifiedImageUrl(),
        fish.getHabitat(),
        records.size(),
        maxSize);
  }
}
