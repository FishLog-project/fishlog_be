package com.fishlog.fishlog_be.global.congestion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * {@link CongestionService} 구현. 시군구 단위 조회 결과에서 <b>오늘자 행만</b> 추려 관광지명→지수 맵으로 만들고, 시군구별로 Redis에 캐시한다.
 * → docs/external.md §2
 *
 * <p><b>TTL이 고정 시간이 아니라 "자정까지"인 이유:</b> 집중률은 날짜 단위 예측값이라 날이 바뀌는 순간 통째로 무의미해진다. 12시간 같은 고정 TTL을 쓰면
 * 자정 직전에 채운 캐시가 다음 날 낮까지 어제 값을 들고 있게 된다. 키에도 날짜를 넣어 이중으로 막는다.
 *
 * <p><b>"데이터 없음"도 캐시한다.</b> 전남처럼 데이터셋에 아예 없는 지역은 매 요청 외부를 두드려도 영원히 빈 결과라, 캐시하지 않으면 그 지역 사용자만 매번 외부
 * 호출 비용을 문다. 다만 <b>호출 실패는 캐시하지 않는다</b> — 일시적 장애를 하루 종일 붙잡아두면 안 된다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CongestionServiceImpl implements CongestionService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter BASE_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String KEY_PREFIX = "congestion:";

  /** 자정 직전 요청이 0에 가까운 TTL을 받는 것을 막는 하한. */
  private static final Duration MIN_TTL = Duration.ofMinutes(1);

  private final CongestionClient congestionClient;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public Map<String, Double> getTodayRates(String areaCd, String signguCd) {
    if (isBlank(areaCd) || isBlank(signguCd)) {
      return Map.of();
    }
    String today = LocalDate.now(KST).format(BASE_YMD);
    String key = KEY_PREFIX + signguCd + ":" + today;

    String cached = safeGet(key);
    if (cached != null) {
      try {
        return objectMapper.readValue(cached, new TypeReference<Map<String, Double>>() {});
      } catch (Exception e) {
        log.warn("[congestion] 캐시 역직렬화 실패, 재적재 시도: {}", e.getMessage());
      }
    }
    return fetchAndCache(areaCd, signguCd, today, key);
  }

  private Map<String, Double> fetchAndCache(
      String areaCd, String signguCd, String today, String key) {
    List<CongestionRate> all;
    try {
      all = congestionClient.fetchBySigngu(areaCd, signguCd);
    } catch (Exception e) {
      // graceful: 외부 실패는 전파하지 않고 혼잡도 없이 응답한다. 실패는 캐시하지 않는다.
      log.warn("[congestion] 집중률 조회 실패, 혼잡도 생략(signguCd={}): {}", signguCd, e.getMessage());
      return Map.of();
    }

    Map<String, Double> todayRates = new HashMap<>();
    for (CongestionRate r : all) {
      if (today.equals(r.baseYmd())) {
        todayRates.put(r.tAtsNm(), r.rate());
      }
    }
    if (todayRates.isEmpty()) {
      log.info("[congestion] 집중률 데이터 없음(signguCd={}) — 빈 결과를 당일 캐시", signguCd);
    }
    safeSet(key, todayRates);
    return todayRates;
  }

  /** 오늘 자정(KST)까지 남은 시간. 날이 바뀌면 값이 통째로 무의미해지므로 그 시점에 만료시킨다. */
  private Duration ttlUntilMidnight() {
    ZonedDateTime now = ZonedDateTime.now(KST);
    ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
    Duration ttl = Duration.between(now, midnight);
    return ttl.compareTo(MIN_TTL) < 0 ? MIN_TTL : ttl;
  }

  private String safeGet(String key) {
    try {
      return redis.opsForValue().get(key);
    } catch (Exception e) {
      log.warn("[congestion] Redis 조회 실패, 외부 재적재로 대체: {}", e.getMessage());
      return null;
    }
  }

  private void safeSet(String key, Map<String, Double> value) {
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttlUntilMidnight());
    } catch (Exception e) {
      log.warn("[congestion] 캐시 저장 실패(무시하고 진행): {}", e.getMessage());
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
