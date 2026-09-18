package com.fishlog.fishlog_be.global.congestion;

import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * {@link CongestionService} 구현. 시군구 단위 조회 결과에서 <b>오늘자 행만</b> 추려 관광지명→지수 맵으로 만든다. → docs/external.md
 * §2-1
 *
 * <p><b>저장하지 않는다 — 매 요청 실시간 호출한다.</b> 관광공사 데이터를 DB·Redis에 적재해두고 그걸로 응답하면 안 된다는 것이 이 프로젝트의 제품
 * 제약이고(§2의 TourAPI와 동일), 관광데이터 활용 공모전 규칙이기도 하다. 쿼터를 아끼려고 캐시를 두면 "우리 저장소에서 서빙"이 되어 규칙에 어긋난다.
 *
 * <p>대신 <b>한 요청 안에서의 중복 호출만</b> 줄인다 — 목록에 같은 시군구 장소가 여러 개 있어도 그 요청 동안 한 번만 부른다(호출부 {@code
 * TourServiceImpl}가 시군구별로 모아 호출). 이건 저장이 아니라 같은 요청 내 중복 제거라 실시간성을 해치지 않는다.
 *
 * <p><b>실패해도 예외를 던지지 않는다.</b> 혼잡도는 관광 목록에 곁들이는 부가 정보라, 외부 장애·쿼터 소진 때문에 목록 조회 자체가 실패하면 안 된다. 실패는 빈
 * 맵으로 떨어지고 호출부는 혼잡도 없이 응답한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CongestionServiceImpl implements CongestionService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter BASE_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final CongestionClient congestionClient;

  @Override
  public Map<String, Double> getTodayRates(String areaCd, String signguCd) {
    if (isBlank(areaCd) || isBlank(signguCd)) {
      return Map.of();
    }

    List<CongestionRate> all;
    try {
      all = congestionClient.fetchBySigngu(areaCd, signguCd);
    } catch (Exception e) {
      // graceful: 외부 실패는 전파하지 않고 혼잡도 없이 응답한다.
      log.warn("[congestion] 집중률 조회 실패, 혼잡도 생략(signguCd={}): {}", signguCd, e.getMessage());
      return Map.of();
    }

    // 집중률 API는 baseYmd 를 요청 파라미터로 받지 않아 항상 30일치가 온다. 당일 필터는 여기서 한다.
    String today = LocalDate.now(KST).format(BASE_YMD);
    Map<String, Double> todayRates = new HashMap<>();
    for (CongestionRate r : all) {
      if (today.equals(r.baseYmd())) {
        todayRates.put(r.tAtsNm(), r.rate());
      }
    }
    return todayRates;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
