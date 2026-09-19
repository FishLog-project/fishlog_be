package com.fishlog.fishlog_be.domain.tour.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fishlog.fishlog_be.domain.tour.dto.NearbyTourResponse;
import com.fishlog.fishlog_be.domain.tour.dto.TourSpotResponse;
import com.fishlog.fishlog_be.global.congestion.CongestionService;
import com.fishlog.fishlog_be.global.tour.TourApiClient;
import com.fishlog.fishlog_be.global.tour.dto.TourApiItem;
import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 관광 목록에 혼잡도를 결합하는 조립 규칙.
 *
 * <p>검증 대상은 값 자체가 아니라 <b>외부를 몇 번 부르는가</b>다. 일 호출 한도가 1,000건이라, 항목마다 부르는 코드와 시군구마다 부르는 코드는 같은 응답을
 * 내면서 비용이 30배 차이 난다.
 */
@ExtendWith(MockitoExtension.class)
class TourServiceImplCongestionTest {

  @Mock private TourApiClient tourApiClient;
  @Mock private CongestionService congestionService;

  @InjectMocks private TourServiceImpl service;

  private static TourApiItem item(String title, String regnCd, String signguCd) {
    return new TourApiItem(title, null, null, "주소", null, 126.4, 35.6, regnCd, signguCd);
  }

  private void givenTourItems(TourApiItem... items) {
    when(tourApiClient.search(anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt(), anyInt()))
        .thenReturn(new TourApiResult(items.length, 1, 30, List.of(items)));
  }

  @Test
  @DisplayName("같은 시군구 항목이 여러 개여도 집중률은 한 번만 조회한다")
  void queriesOncePerSigngu() {
    givenTourItems(item("채석강", "52", "800"), item("개암사", "52", "800"), item("수성당", "52", "800"));
    when(congestionService.getTodayRates("52", "52800"))
        .thenReturn(Map.of("채석강 (전북 서해안 국가지질공원)", 39.61, "개암사", 26.31, "수성당", 55.0));

    service.getNearbyTours("관광지", 35.6, 126.4, 5000, 1);

    verify(congestionService, times(1)).getTodayRates("52", "52800");
  }

  @Test
  @DisplayName("시군구가 섞이면 시군구 수만큼만 조회한다")
  void queriesPerDistinctSigngu() {
    givenTourItems(item("채석강", "52", "800"), item("개암사", "52", "800"), item("선유도", "52", "710"));
    when(congestionService.getTodayRates(anyString(), anyString())).thenReturn(Map.of());

    service.getNearbyTours("관광지", 35.6, 126.4, 5000, 1);

    verify(congestionService, times(1)).getTodayRates("52", "52800");
    verify(congestionService, times(1)).getTodayRates("52", "52710");
  }

  @Test
  @DisplayName("숙박·음식점은 집중률을 아예 조회하지 않고 congestion은 null이다")
  void skipsNonAttractionCategories() {
    givenTourItems(item("해운대암소갈비집", "26", "350"));

    NearbyTourResponse response = service.getNearbyTours("음식점", 35.1, 129.1, 5000, 1);

    assertThat(response.items()).singleElement().extracting(TourSpotResponse::congestion).isNull();
    verifyNoInteractions(congestionService);
  }

  @Test
  @DisplayName("매칭된 장소만 혼잡도가 붙고 나머지는 null이다")
  void attachesOnlyMatched() {
    givenTourItems(item("채석강", "52", "800"), item("격포항", "52", "800"));
    when(congestionService.getTodayRates("52", "52800"))
        .thenReturn(Map.of("채석강 (전북 서해안 국가지질공원)", 39.61));

    NearbyTourResponse response = service.getNearbyTours("관광지", 35.6, 126.4, 5000, 1);

    assertThat(response.items()).hasSize(2);
    assertThat(response.items().get(0).congestion()).isNotNull();
    assertThat(response.items().get(0).congestion().rate()).isEqualTo(39.61);
    assertThat(response.items().get(0).congestion().level()).isEqualTo("여유");
    assertThat(response.items().get(0).congestion().baseDate())
        .isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));
    assertThat(response.items().get(1).congestion()).isNull();
  }

  @Test
  @DisplayName("법정동 코드가 없는 장소는 조회 없이 null이다")
  void skipsItemsWithoutLegalDongCode() {
    givenTourItems(item("코드없는장소", null, null));

    NearbyTourResponse response = service.getNearbyTours("관광지", 35.6, 126.4, 5000, 1);

    assertThat(response.items()).singleElement().extracting(TourSpotResponse::congestion).isNull();
    verifyNoInteractions(congestionService);
  }
}
