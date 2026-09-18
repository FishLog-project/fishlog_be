package com.fishlog.fishlog_be.global.congestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.tour.TourErrorCode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 당일 집중률 조회 동작.
 *
 * <p>핵심은 세 가지다 — ① 30일치 중 <b>오늘 것만</b> 남긴다, ② <b>매 호출 외부를 실시간으로 부른다</b>(관광공사 데이터를 저장했다가 서빙하면 제품
 * 제약·공모전 규칙에 어긋난다), ③ 혼잡도 조회 실패가 <b>예외로 새어나가지 않는다</b>(관광 목록이 같이 죽으면 안 된다).
 */
@ExtendWith(MockitoExtension.class)
class CongestionServiceImplTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Mock private CongestionClient congestionClient;

  @InjectMocks private CongestionServiceImpl service;

  private String today;
  private String tomorrow;

  @BeforeEach
  void setUp() {
    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
    today = LocalDate.now(KST).format(f);
    tomorrow = LocalDate.now(KST).plusDays(1).format(f);
  }

  @Test
  @DisplayName("30일치 중 오늘 행만 남겨 관광지명→집중률 맵을 만든다")
  void keepsOnlyToday() {
    when(congestionClient.fetchBySigngu("52", "52800"))
        .thenReturn(
            List.of(
                new CongestionRate(today, "채석강 (전북 서해안 국가지질공원)", 39.61),
                new CongestionRate(today, "개암사", 26.31),
                new CongestionRate(tomorrow, "채석강 (전북 서해안 국가지질공원)", 96.92),
                new CongestionRate(tomorrow, "개암사", 35.26)));

    Map<String, Double> result = service.getTodayRates("52", "52800");

    assertThat(result)
        .containsOnly(Map.entry("채석강 (전북 서해안 국가지질공원)", 39.61), Map.entry("개암사", 26.31));
  }

  @Test
  @DisplayName("같은 시군구를 다시 물어도 저장분을 주지 않고 매번 실시간 호출한다")
  void alwaysCallsExternalInRealTime() {
    when(congestionClient.fetchBySigngu("52", "52800"))
        .thenReturn(List.of(new CongestionRate(today, "개암사", 26.31)));

    service.getTodayRates("52", "52800");
    service.getTodayRates("52", "52800");
    service.getTodayRates("52", "52800");

    verify(congestionClient, times(3)).fetchBySigngu("52", "52800");
  }

  @Test
  @DisplayName("데이터 없는 지역(전남)도 저장해두지 않는다 — 매번 실제로 확인한다")
  void absentRegionIsNotRemembered() {
    when(congestionClient.fetchBySigngu("12", "12130")).thenReturn(List.of());

    assertThat(service.getTodayRates("12", "12130")).isEmpty();
    assertThat(service.getTodayRates("12", "12130")).isEmpty();

    verify(congestionClient, times(2)).fetchBySigngu("12", "12130");
  }

  @Test
  @DisplayName("외부 호출이 실패해도 예외를 던지지 않고 빈 맵을 준다 — 관광 목록은 200을 유지해야 한다")
  void externalFailureIsSwallowed() {
    when(congestionClient.fetchBySigngu(anyString(), anyString()))
        .thenThrow(new CustomException(TourErrorCode.TOUR_API_UNAVAILABLE));

    assertThat(service.getTodayRates("52", "52800")).isEmpty();
  }

  @Test
  @DisplayName("지역코드가 없으면 외부를 호출하지 않는다 — TourAPI가 법정동 코드를 안 준 장소")
  void blankCodesSkipEverything() {
    assertThat(service.getTodayRates(null, "52800")).isEmpty();
    assertThat(service.getTodayRates("52", null)).isEmpty();
    assertThat(service.getTodayRates("52", " ")).isEmpty();
    verifyNoInteractions(congestionClient);
  }
}
