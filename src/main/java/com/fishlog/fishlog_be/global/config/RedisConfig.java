package com.fishlog.fishlog_be.global.config;

import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 설정. 바다낚시지수 예보를 **반나절(12h) TTL**로 캐싱하기 위한 구성. (#15)
 *
 * <p>연결(host/port)은 Spring Boot 자동설정({@code spring.data.redis.*}, 기본 localhost:6379)을 따르고, 여기서는
 * 직렬화·캐시 TTL만 정의한다. 키는 문자열, 값은 JSON으로 저장한다.
 */
@Configuration
@EnableCaching
public class RedisConfig {

  /** 예보 캐시 기본 TTL — 예보 주기가 predcYmd+오전/오후(반나절)라 12시간으로 둔다. */
  public static final Duration FORECAST_TTL = Duration.ofHours(12);

  /** 수동 캐싱/조회용 RedisTemplate (String 키 + JSON 값). */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    RedisSerializer<String> keySerializer = RedisSerializer.string();
    RedisSerializer<Object> valueSerializer = RedisSerializer.json();

    template.setKeySerializer(keySerializer);
    template.setHashKeySerializer(keySerializer);
    template.setValueSerializer(valueSerializer);
    template.setHashValueSerializer(valueSerializer);
    template.afterPropertiesSet();
    return template;
  }

  /** {@code @Cacheable} 용 캐시 매니저 — 기본 TTL 12시간, 값은 JSON 직렬화, null 미캐싱. */
  @Bean
  public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(FORECAST_TTL)
            .disableCachingNullValues()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    RedisSerializer.string()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(config).build();
  }
}
