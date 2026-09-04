package com.fishlog.fishlog_be.global.tour;

import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;

/** 한국관광공사 TourAPI(KorService2) 위치기반 관광정보 조회 클라이언트. → docs/external.md */
public interface TourApiClient {

  /**
   * {@code locationBasedList2}로 좌표 주변의 특정 콘텐츠 유형 장소를 거리순으로 조회한다.
   *
   * @param contentTypeId TourAPI 콘텐츠 유형(12 관광지 / 32 숙박 / 39 음식점)
   * @param lat 위도(mapY)
   * @param lng 경도(mapX)
   * @param radius 반경(m)
   * @param page 페이지(1-base)
   * @param numOfRows 페이지당 개수
   * @return 페이지 메타 + 장소 목록
   * @throws com.fishlog.fishlog_be.global.exception.CustomException TourAPI 오류/응답 이상/연결 실패 시 {@code
   *     TourErrorCode}
   */
  TourApiResult search(
      int contentTypeId, double lat, double lng, int radius, int page, int numOfRows);
}
