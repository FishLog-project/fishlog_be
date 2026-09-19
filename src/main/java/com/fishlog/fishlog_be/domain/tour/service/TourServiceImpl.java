package com.fishlog.fishlog_be.domain.tour.service;

import com.fishlog.fishlog_be.domain.tour.dto.CongestionResponse;
import com.fishlog.fishlog_be.domain.tour.dto.NearbyTourResponse;
import com.fishlog.fishlog_be.domain.tour.dto.TourSpotResponse;
import com.fishlog.fishlog_be.domain.tour.entity.TourCategory;
import com.fishlog.fishlog_be.domain.tour.policy.CongestionMatcher;
import com.fishlog.fishlog_be.global.congestion.CongestionService;
import com.fishlog.fishlog_be.global.tour.TourApiClient;
import com.fishlog.fishlog_be.global.tour.dto.TourApiItem;
import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** {@link TourService} 구현. 주변 관광 장소를 조회하고, <b>관광지</b>에 한해 당일 혼잡도를 덧붙인다. → docs/external.md §2 */
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

  /** 페이지당 개수(고정). */
  private static final int NUM_OF_ROWS = 30;

  /** TourAPI radius 상한(m). */
  private static final int MAX_RADIUS = 50000; // 5km 반경

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final TourApiClient tourApiClient;
  private final CongestionService congestionService;

  @Override
  public NearbyTourResponse getNearbyTours(
      String type, double lat, double lng, int radius, int page) {
    TourCategory category = TourCategory.from(type); // 잘못된 type → INVALID_TYPE(400)
    int safeRadius = Math.max(1, Math.min(radius, MAX_RADIUS));
    int safePage = Math.max(1, page);
    TourApiResult result =
        tourApiClient.search(category.contentTypeId(), lat, lng, safeRadius, safePage, NUM_OF_ROWS);
    return NearbyTourResponse.of(category, result, toSpots(category, result));
  }

  /**
   * 장소 목록을 응답 DTO로 바꾸면서 혼잡도를 결합한다.
   *
   * <p>혼잡도는 <b>관광지에만</b> 붙인다. 집중률 데이터셋이 관광지만 다뤄 숙박·음식점은 어차피 전부 매칭에 실패하는데, 그걸 확인하려고 외부를 호출하면 일 호출
   * 한도만 태운다.
   *
   * <p>집중률은 시군구 단위로 오므로 <b>목록에 등장한 시군구별로 한 번만</b> 조회해 재사용한다. 반경 5km면 대개 시군구 1~2개라 30건짜리 목록 하나가 외부
   * 호출 0~2회로 끝나고, 그마저도 당일 캐시가 채워진 뒤에는 0회다.
   */
  private List<TourSpotResponse> toSpots(TourCategory category, TourApiResult result) {
    if (category != TourCategory.ATTRACTION) {
      return result.items().stream().map(i -> TourSpotResponse.from(i, null)).toList();
    }
    LocalDate today = LocalDate.now(KST);
    Map<String, Map<String, Double>> ratesBySigngu = new HashMap<>();
    List<TourSpotResponse> spots = new ArrayList<>();
    for (TourApiItem item : result.items()) {
      spots.add(TourSpotResponse.from(item, resolveCongestion(item, ratesBySigngu, today)));
    }
    return spots;
  }

  /** 한 장소의 혼잡도를 구한다. 시군구 코드가 없거나 이름이 매칭되지 않으면 {@code null}. */
  private CongestionResponse resolveCongestion(
      TourApiItem item, Map<String, Map<String, Double>> ratesBySigngu, LocalDate today) {
    String signguCd = item.legalDongSignguCode();
    if (signguCd == null) {
      return null;
    }
    Map<String, Double> rates =
        ratesBySigngu.computeIfAbsent(
            signguCd, cd -> congestionService.getTodayRates(item.lDongRegnCd(), cd));
    Double rate = CongestionMatcher.match(item.title(), rates);
    return rate == null ? null : CongestionResponse.of(rate, today);
  }
}
