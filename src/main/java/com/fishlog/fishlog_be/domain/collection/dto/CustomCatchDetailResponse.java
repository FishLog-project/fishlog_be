package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 도감 외 어종 <b>1종</b>에 대한 내 기록 요약(서식지 + 잡은 횟수 + 최대 크기 + 최근 사진). {@link CatchRecordResponse}(도감 어종
 * 상세)와 같은 스펙이며, 화면이 두 상세를 <b>같은 썸네일 4칸 + 오버레이 컴포넌트</b>로 그릴 수 있게 필드를 맞췄다.
 *
 * <p>도감 상세에 없는 {@code name}이 하나 더 있다. 도감은 클라이언트가 이미 {@code fishId}로 어종을 알고 있지만, 사용자가 만든 어종은 이름이
 * 서버에만 있어 상세를 단독으로 열면 표시할 이름이 없기 때문이다.
 *
 * <p><b>{@code catchCount}·{@code maxSize}는 자르지 않은 전체 기록 기준</b>이고 {@code recentCatches}만 최대 4장으로
 * 제한된다 — 최대 크기가 잘려 나간 기록에 있을 수 있으므로 응답에 온 사진 4장에서 계산한 값과 다를 수 있다(서버 값이 맞다).
 */
@Schema(title = "CustomCatchDetailResponse DTO", description = "도감 외 어종 1종에 대한 내 기록 요약")
public record CustomCatchDetailResponse(
    @Schema(description = "도감 외 어종 ID", example = "3") Long customFishId,
    @Schema(description = "사용자가 수기 입력한 어종명", example = "쏘가리") String name,
    @Schema(description = "주요 서식지(수기 입력, 미입력이면 null)", example = "강") String habitat,
    @Schema(description = "이 어종을 등록한 총 횟수. 사진 4장 제한과 무관한 전체 값", example = "3") int catchCount,
    @Schema(description = "이 어종으로 등록한 것 중 가장 큰 크기(cm)", example = "41.0") Double maxSize,
    @Schema(description = "최근 사진(최신순, 최대 4장). 각 항목에 그때의 크기·위치 포함")
        List<CustomCatchPhotoResponse> recentCatches) {

  /**
   * @param fish 조회한 사용자별 어종(이름·서식지의 출처)
   * @param catchCount 자르기 전 전체 등록 횟수
   * @param maxSize 자르기 전 전체 기록의 최대 크기(cm)
   * @param recentRecords 최신순으로 잘라온 기록(최대 4건)
   */
  public static CustomCatchDetailResponse of(
      CustomFish fish, int catchCount, Double maxSize, List<CustomCatchRecord> recentRecords) {
    List<CustomCatchPhotoResponse> photos =
        recentRecords.stream().map(CustomCatchPhotoResponse::from).toList();
    return new CustomCatchDetailResponse(
        fish.getId(), fish.getName(), fish.getHabitat(), catchCount, maxSize, photos);
  }
}
