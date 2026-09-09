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
 * <p>{@code imageUrl}은 어종·기록과 무관하게 <b>항상 같은 기본 이미지</b>({@code basic_image})다. 사용자가 만든 어종에는 도감 같은 공식
 * 이미지가 없고, 그리드는 도감 그리드와 나란히 놓이는 화면이라 칸마다 제각각인 실사 사진보다 통일된 아이콘이 낫다는 판단이다. 사용자가 <b>자기가 찍은 사진</b>을 보고
 * 싶으면 칸을 눌러 상세({@code GET /api/collections/custom?customFishId=})로 들어간다. → docs/media.md
 */
@Schema(title = "CustomDexEntryResponse DTO", description = "내 도감 외 어종 항목(어종 + 잡은 횟수·최대 크기)")
public record CustomDexEntryResponse(
    @Schema(description = "도감 외 어종 ID(상세 조회 시 customFishId 로 사용)", example = "3") Long id,
    @Schema(description = "사용자가 수기 입력한 어종명", example = "쏘가리") String name,
    @Schema(
            description = "대표 이미지 URL — 도감 외 어종 공통 기본 이미지(파일 없으면 null)",
            example = "http://localhost:8080/images/fish/basic_image.png")
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
   * @param records 그 어종의 기록 전체(횟수·최대 크기의 출처). 순서는 상관없다 — 대표 이미지로 사진을 쓰지 않기 때문이다.
   * @param basicImageUrl 도감 외 어종 공통 기본 이미지 URL({@link
   *     com.fishlog.fishlog_be.global.image.FishImageService#getBasicImageUrl()})
   */
  public static CustomDexEntryResponse of(
      CustomFish fish, List<CustomCatchRecord> records, String basicImageUrl) {
    // 최대 크기는 전체 기록에서 구한다. size 는 NOT NULL 이고 어종 행은 기록이 있어야 생기므로 항상 값이 있다.
    Double maxSize =
        records.stream().map(CustomCatchRecord::getSize).max(Double::compare).orElse(null);
    return new CustomDexEntryResponse(
        fish.getId(), fish.getName(), basicImageUrl, fish.getHabitat(), records.size(), maxSize);
  }
}
