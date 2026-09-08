package com.fishlog.fishlog_be.domain.tour.service;

import com.fishlog.fishlog_be.domain.tour.dto.NearbyTourResponse;

/** 사용자 위치 기반 주변 관광 정보 조회 서비스. */
public interface TourService {

  /**
   * 좌표 주변의 특정 카테고리 관광 장소를 거리순으로 조회한다(페이지당 30개 고정).
   *
   * @param type 카테고리 라벨(관광지/숙박/음식점)
   * @param lat 위도
   * @param lng 경도
   * @param radius 반경(m)
   * @param page 페이지(1-base)
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 지원하지 않는 {@code type}이거나 TourAPI
   *     오류 시 {@code TourErrorCode}
   */
  NearbyTourResponse getNearbyTours(String type, double lat, double lng, int radius, int page);
}
