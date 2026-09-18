package com.fishlog.fishlog_be.global.congestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fishlog.fishlog_be.domain.tour.policy.CongestionMatcher;
import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import com.fishlog.fishlog_be.global.tour.TourApiClientImpl;
import com.fishlog.fishlog_be.global.tour.dto.TourApiItem;
import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * <b>실제 외부 API를 호출하는 통합 테스트.</b> 기본적으로 실행되지 않는다 — 환경변수 {@code FISHLOG_EXTERNAL_IT=true}일 때만 켜진다.
 *
 * <p>모킹 테스트가 검증하지 못하는 것을 확인한다: URI 조립, serviceKey 인코딩, RestClient 타임아웃, 그리고 <b>TourAPI가 준 법정동 코드가
 * 집중률 API에서 실제로 통하는지</b>. 계약이 바뀌면 여기서만 깨진다.
 *
 * <pre>
 *   FISHLOG_EXTERNAL_IT=true ./gradlew test --tests '*CongestionLiveIntegrationTest*' -i
 * </pre>
 *
 * <p>일 호출 한도(1,000건)를 쓰므로 상시 실행(CI)에 넣지 않는다.
 */
@EnabledIfEnvironmentVariable(named = "FISHLOG_EXTERNAL_IT", matches = "true")
class CongestionLiveIntegrationTest {

  /** 격포항(전북 부안) 좌표 — 낚시 스팟 기준 실제 사용 시나리오. */
  private static final double LAT = 35.6167;

  private static final double LNG = 126.4633;

  private static TourApiClientImpl tourClient;
  private static CongestionClientImpl congestionClient;

  @BeforeAll
  static void setUp() throws IOException {
    Properties p = new Properties();
    try (FileInputStream in =
        new FileInputStream("src/main/resources/application-local.properties")) {
      p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
    }
    String key = p.getProperty("external.tour.service-key");
    assertThat(key).as("external.tour.service-key 가 설정돼 있어야 한다").isNotBlank();

    RestClient rest = RestClient.builder().requestFactory(factory()).build();
    tourClient =
        new TourApiClientImpl(rest, p.getProperty("external.tour.base-url"), key, "fishlog");
    congestionClient =
        new CongestionClientImpl(
            rest,
            "https://apis.data.go.kr/B551011/TatsCnctrRateService/tatsCnctrRatedList",
            key,
            "fishlog");
  }

  private static SimpleClientHttpRequestFactory factory() {
    SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(Duration.ofSeconds(5));
    f.setReadTimeout(Duration.ofSeconds(15));
    return f;
  }

  @Test
  @DisplayName("실 호출: TourAPI가 준 법정동 코드로 집중률을 받아 장소에 붙인다")
  void endToEndAgainstRealApis() {
    // 1) 실제 TourAPI 호출 — 법정동 코드가 실려 오는지
    TourApiResult tour = tourClient.search(12, LAT, LNG, 5000, 1, 15);
    assertThat(tour.items()).as("반경 5km 안에 관광지가 있어야 한다").isNotEmpty();

    TourApiItem first = tour.items().get(0);
    assertThat(first.lDongRegnCd()).as("법정동 시도코드").isNotBlank();
    assertThat(first.legalDongSignguCode()).as("법정동 시군구코드 5자리").hasSize(5);
    System.out.printf(
        "%n[1] TourAPI %d건, 법정동 %s / %s%n",
        tour.items().size(), first.lDongRegnCd(), first.legalDongSignguCode());

    // 2) 그 코드로 실제 집중률 호출
    List<CongestionRate> rates =
        congestionClient.fetchBySigngu(first.lDongRegnCd(), first.legalDongSignguCode());
    assertThat(rates).as("부안군은 집중률 데이터가 있는 지역이다").isNotEmpty();

    String today =
        LocalDate.now(ZoneId.of("Asia/Seoul")).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    Map<String, Double> todayRates =
        rates.stream()
            .filter(r -> today.equals(r.baseYmd()))
            .collect(Collectors.toMap(CongestionRate::tAtsNm, CongestionRate::rate, (a, b) -> a));
    assertThat(todayRates).as("오늘자 예측이 포함돼 있어야 한다").isNotEmpty();
    System.out.printf("[2] 집중률 %d행 수신, 오늘(%s) 관광지 %d곳%n", rates.size(), today, todayRates.size());

    // 3) 매칭까지 — 실제로 화면에 뜰 값
    long matched = 0;
    System.out.println("[3] 장소별 결합 결과");
    for (TourApiItem item : tour.items()) {
      Double rate = CongestionMatcher.match(item.title(), todayRates);
      if (rate != null) {
        matched++;
      }
      System.out.printf(
          "      %-28s %s%n", item.title(), rate == null ? "-" : String.format("%.1f", rate));
    }
    System.out.printf("    → %d/%d 매칭%n%n", matched, tour.items().size());
    assertThat(matched).as("부안 격포항 주변은 최소 1곳은 붙어야 한다").isGreaterThan(0);
  }

  @Test
  @DisplayName("실 호출: 데이터가 없는 지역(전남)은 예외 없이 빈 리스트다")
  void absentRegionReturnsEmpty() {
    assertThat(congestionClient.fetchBySigngu("12", "12130")).isEmpty();
  }
}
