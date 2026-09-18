package com.fishlog.fishlog_be.global.congestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.tour.TourErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 당일 집중률 캐시 동작.
 *
 * <p>핵심은 세 가지다 — ① 30일치 중 <b>오늘 것만</b> 남긴다, ② 혼잡도 조회 실패가 <b>예외로 새어나가지 않는다</b>(관광 목록이 같이 죽으면 안 된다),
 * ③ <b>"데이터 없음"은 캐시하고 "호출 실패"는 캐시하지 않는다</b> — 전남처럼 영원히 빈 지역과 일시 장애를 구분해야 한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CongestionServiceImplTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Mock private CongestionClient congestionClient;
  @Mock private StringRedisTemplate redis;
  @Mock private ValueOperations<String, String> valueOps;

  @InjectMocks private CongestionServiceImpl service;

  private String today;
  private String tomorrow;

  @BeforeEach
  void setUp() {
    DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMdd");
    today = LocalDate.now(KST).format(f);
    tomorrow = LocalDate.now(KST).plusDays(1).format(f);
    when(redis.opsForValue()).thenReturn(valueOps);
  }

  @Test
  @DisplayName("30일치 중 오늘 행만 남겨 관광지명→집중률 맵을 만든다")
  void keepsOnlyToday() {
    when(valueOps.get(anyString())).thenReturn(null);
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
  @DisplayName("캐시가 있으면 외부를 호출하지 않는다")
  void cacheHitSkipsExternalCall() {
    when(valueOps.get("congestion:52800:" + today)).thenReturn("{\"개암사\":26.31}");

    assertThat(service.getTodayRates("52", "52800")).containsOnly(Map.entry("개암사", 26.31));
    verifyNoInteractions(congestionClient);
  }

  @Test
  @DisplayName("외부 호출이 실패해도 예외를 던지지 않고 빈 맵을 준다 — 관광 목록은 200을 유지해야 한다")
  void externalFailureIsSwallowed() {
    when(valueOps.get(anyString())).thenReturn(null);
    when(congestionClient.fetchBySigngu(anyString(), anyString()))
        .thenThrow(new CustomException(TourErrorCode.TOUR_API_UNAVAILABLE));

    assertThat(service.getTodayRates("52", "52800")).isEmpty();
  }

  @Test
  @DisplayName("호출 실패는 캐시하지 않는다 — 일시 장애를 하루 종일 붙잡으면 안 된다")
  void failureIsNotCached() {
    when(valueOps.get(anyString())).thenReturn(null);
    when(congestionClient.fetchBySigngu(anyString(), anyString()))
        .thenThrow(new CustomException(TourErrorCode.TOUR_API_ERROR));

    service.getTodayRates("52", "52800");

    verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  @DisplayName("데이터 없는 지역(전남)은 빈 결과를 캐시해 매 요청 외부를 두드리지 않는다")
  void absentRegionIsCached() {
    when(valueOps.get(anyString())).thenReturn(null);
    when(congestionClient.fetchBySigngu("12", "12130")).thenReturn(List.of());

    assertThat(service.getTodayRates("12", "12130")).isEmpty();
    verify(valueOps).set(anyString(), anyString(), any(Duration.class));
  }

  @Test
  @DisplayName("캐시 키에 날짜가 들어가고 TTL은 자정까지다(24시간을 넘지 않는다)")
  void cacheKeyAndTtl() {
    when(valueOps.get(anyString())).thenReturn(null);
    when(congestionClient.fetchBySigngu("52", "52800"))
        .thenReturn(List.of(new CongestionRate(today, "개암사", 26.31)));

    service.getTodayRates("52", "52800");

    ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
    verify(valueOps).set(key.capture(), anyString(), ttl.capture());

    assertThat(key.getValue()).isEqualTo("congestion:52800:" + today);
    assertThat(ttl.getValue()).isPositive().isLessThanOrEqualTo(Duration.ofHours(24));
  }

  @Test
  @DisplayName("지역코드가 없으면 외부를 호출하지 않는다 — TourAPI가 법정동 코드를 안 준 장소")
  void blankCodesSkipEverything() {
    assertThat(service.getTodayRates(null, "52800")).isEmpty();
    assertThat(service.getTodayRates("52", null)).isEmpty();
    assertThat(service.getTodayRates("52", " ")).isEmpty();
    verifyNoInteractions(congestionClient);
  }

  @Test
  @DisplayName("Redis가 죽어도 외부 재적재로 대체한다")
  void redisFailureFallsBackToExternal() {
    when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
    when(congestionClient.fetchBySigngu("52", "52800"))
        .thenReturn(List.of(new CongestionRate(today, "개암사", 26.31)));

    assertThat(service.getTodayRates("52", "52800")).containsOnly(Map.entry("개암사", 26.31));
  }
}
