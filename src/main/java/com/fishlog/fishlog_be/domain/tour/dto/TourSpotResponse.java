package com.fishlog.fishlog_be.domain.tour.dto;

import com.fishlog.fishlog_be.global.tour.dto.TourApiItem;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 주변 관광 장소 1건(응답).
 *
 * @param title 장소명
 * @param firstImage 대표 이미지 URL (없으면 null)
 * @param firstImage2 썸네일 이미지 URL (없으면 null)
 * @param addr1 기본 주소 (없으면 null)
 * @param addr2 상세 주소 (없으면 null)
 * @param mapX 경도 (없으면 null)
 * @param mapY 위도 (없으면 null)
 * @param congestion 당일 혼잡도 (붙일 수 없으면 null — 아래 참고)
 */
@Schema(description = "주변 관광 장소")
public record TourSpotResponse(
    @Schema(example = "채석강") String title,
    @Schema(description = "대표 이미지 URL(없으면 null)") String firstImage,
    @Schema(description = "썸네일 이미지 URL(없으면 null)") String firstImage2,
    @Schema(example = "전북특별자치도 부안군 변산면 격포리") String addr1,
    @Schema(description = "상세 주소(없으면 null)") String addr2,
    @Schema(description = "경도", example = "126.4699") Double mapX,
    @Schema(description = "위도", example = "35.6204") Double mapY,
    @Schema(
            description =
                """
                당일 혼잡도. 다음 경우 null이며, 화면은 혼잡도 칩을 숨긴다.
                (1) 숙박·음식점 — 집중률 데이터셋이 관광지만 다룬다
                (2) 집중률 데이터셋에 없는 장소 — 항·포구·리조트·신규시설 다수
                (3) 데이터가 없는 지역 — 전남 전역
                (4) 집중률 API 장애·쿼터 소진 — 목록 응답 자체는 200을 유지한다""")
        CongestionResponse congestion) {

  public static TourSpotResponse from(TourApiItem i, CongestionResponse congestion) {
    return new TourSpotResponse(
        i.title(),
        i.firstImage(),
        i.firstImage2(),
        i.addr1(),
        i.addr2(),
        i.mapX(),
        i.mapY(),
        congestion);
  }
}
