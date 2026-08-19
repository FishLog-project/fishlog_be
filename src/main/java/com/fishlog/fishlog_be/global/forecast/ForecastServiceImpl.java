package com.fishlog.fishlog_be.global.forecast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.config.RedisConfig;
import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * {@link ForecastService} 구현. 전체 예보를 스팟명→예보목록 맵으로 만들어 단일 Redis 키에 12h TTL로 캐시한다. → docs/external.md
 * §1
 *
 * <p>캐시 miss 시 {@link FishingIndexClient}로 재적재하되, 외부 호출이 실패하면 캐시하지 않고 빈 결과를 반환한다(상세 응답은 예보 없이도
 * 200).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastServiceImpl implements ForecastService {

  private static final String CACHE_KEY = "forecast:fishing-index:all";

  private final FishingIndexClient fishingIndexClient;
  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public List<SpotForecast> getForecast(String spotName) {
    if (spotName == null || spotName.isBlank()) {
      return List.of();
    }
    Map<String, List<SpotForecast>> byName = loadAll();
    return byName.getOrDefault(spotName, List.of());
  }

  /** 캐시에서 전체 예보 맵을 읽고, 없으면 외부에서 재적재해 캐시한다. 실패 시 빈 맵. */
  private Map<String, List<SpotForecast>> loadAll() {
    String cached = safeGet();
    if (cached != null) {
      try {
        return objectMapper.readValue(cached, new TypeReference<>() {});
      } catch (Exception e) {
        log.warn("[forecast] 캐시 역직렬화 실패, 재적재 시도: {}", e.getMessage());
      }
    }
    return fetchAndCache();
  }

  private Map<String, List<SpotForecast>> fetchAndCache() {
    List<SpotForecast> all;
    try {
      all = fishingIndexClient.fetchAll();
    } catch (Exception e) {
      // graceful: 외부 실패는 예외 전파하지 않고 빈 결과(상세는 예보 null로 응답).
      log.warn("[forecast] 바다낚시지수 호출 실패, 예보 생략: {}", e.getMessage());
      return Map.of();
    }
    Map<String, List<SpotForecast>> byName =
        all.stream()
            .filter(f -> f.seafsPstnNm() != null)
            .collect(Collectors.groupingBy(SpotForecast::seafsPstnNm));
    try {
      redis
          .opsForValue()
          .set(CACHE_KEY, objectMapper.writeValueAsString(byName), RedisConfig.FORECAST_TTL);
    } catch (Exception e) {
      log.warn("[forecast] 캐시 저장 실패(무시하고 진행): {}", e.getMessage());
    }
    return byName;
  }

  private String safeGet() {
    try {
      return redis.opsForValue().get(CACHE_KEY);
    } catch (Exception e) {
      log.warn("[forecast] Redis 조회 실패, 외부 재적재로 대체: {}", e.getMessage());
      return null;
    }
  }
}
