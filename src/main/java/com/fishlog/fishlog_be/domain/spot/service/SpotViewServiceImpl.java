package com.fishlog.fishlog_be.domain.spot.service;

import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SpotViewService} 구현. GET 응답을 막지 않도록 {@code @Async}로 처리하고, 사용자/IP별 1일 1회만 집계한다(Redis
 * dedup).
 *
 * <p>증가는 {@code SpotRepository.incrementViewCount}(원자적 UPDATE)로 한다. 비동기 스레드에서 실패해도 조회 응답에는 영향이 없도록
 * 예외를 삼키고 로깅만 한다. → docs/spec.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpotViewServiceImpl implements SpotViewService {

  private static final String DEDUP_KEY = "spot:view:dedup:"; // spot:view:dedup:{spotId}:{식별자}
  private static final Duration DEDUP_TTL = Duration.ofHours(24);

  private final SpotRepository spotRepository;
  private final StringRedisTemplate redis;

  @Override
  @Async
  @Transactional
  public void recordView(Long spotId, Long userId, String clientIp) {
    try {
      String identity = userId != null ? "u:" + userId : "ip:" + clientIp;
      String key = DEDUP_KEY + spotId + ":" + identity;
      // 처음 조회(키 신규 생성)면 true → 증가. 24h 내 재조회면 false → 무시.
      Boolean firstView = redis.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
      if (Boolean.TRUE.equals(firstView)) {
        spotRepository.incrementViewCount(spotId);
      }
    } catch (Exception e) {
      // 조회수 집계 실패는 상세 조회 자체에 영향 없음(비동기) — 로깅만.
      log.warn("[spot-view] 조회수 집계 실패 spotId={} ({})", spotId, e.getMessage());
    }
  }
}
