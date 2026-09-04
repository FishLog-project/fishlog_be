package com.fishlog.fishlog_be.domain.tour.dto;

import com.fishlog.fishlog_be.global.tour.dto.TourApiItem;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주변 관광 장소 1건(응답). 요청하신 7필드만 노출한다.
 *
 * @param title 장소명
 * @param firstImage 대표 이미지 URL (없으면 null)
 * @param firstImage2 썸네일 이미지 URL (없으면 null)
 * @param addr1 기본 주소 (없으면 null)
 * @param addr2 상세 주소 (없으면 null)
 * @param mapX 경도 (없으면 null)
 * @param mapY 위도 (없으면 null)
 */
@Schema(description = "주변 관광 장소")
public record TourSpotResponse(
    @Schema(example = "해운대해수욕장") String title,
    @Schema(description = "대표 이미지 URL(없으면 null)") String firstImage,
    @Schema(description = "썸네일 이미지 URL(없으면 null)") String firstImage2,
    @Schema(example = "부산광역시 해운대구 우동") String addr1,
    @Schema(description = "상세 주소(없으면 null)") String addr2,
    @Schema(description = "경도", example = "129.1603") Double mapX,
    @Schema(description = "위도", example = "35.1587") Double mapY) {

  public static TourSpotResponse from(TourApiItem i) {
    return new TourSpotResponse(
        i.title(), i.firstImage(), i.firstImage2(), i.addr1(), i.addr2(), i.mapX(), i.mapY());
  }
}
