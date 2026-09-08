package com.fishlog.fishlog_be.domain.tour.service;

import com.fishlog.fishlog_be.domain.tour.dto.NearbyTourResponse;
import com.fishlog.fishlog_be.domain.tour.entity.TourCategory;
import com.fishlog.fishlog_be.global.tour.TourApiClient;
import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

  /** 페이지당 개수(고정). */
  private static final int NUM_OF_ROWS = 30;

  /** TourAPI radius 상한(m). */
  private static final int MAX_RADIUS = 50000; // 5km 반경

  private final TourApiClient tourApiClient;

  @Override
  public NearbyTourResponse getNearbyTours(
      String type, double lat, double lng, int radius, int page) {
    TourCategory category = TourCategory.from(type); // 잘못된 type → INVALID_TYPE(400)
    int safeRadius = Math.max(1, Math.min(radius, MAX_RADIUS));
    int safePage = Math.max(1, page);
    TourApiResult result =
        tourApiClient.search(category.contentTypeId(), lat, lng, safeRadius, safePage, NUM_OF_ROWS);
    return NearbyTourResponse.of(category, result);
  }
}
