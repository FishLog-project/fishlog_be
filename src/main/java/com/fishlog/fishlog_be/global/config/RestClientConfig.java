package com.fishlog.fishlog_be.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 HTTP 호출용 {@link RestClient} 설정. 바다낚시지수 예보 호출({@code global/forecast})과 어종 분류 모델 호출({@code
 * global/ai})에 쓰이며, 연결·읽기 타임아웃을 둬 외부 지연이 응답을 오래 붙잡지 않게 한다. → docs/external.md
 *
 * <p><b>빈으로 등록해 재사용하는 것이 핵심이다.</b> 요청마다 RestClient 를 새로 만들면 커넥션이 매번 버려져 분류 처리량이 15건/초 → 3.4건/초로
 * 떨어지는 것이 실측됐다.
 */
@Configuration
public class RestClientConfig {

  @Value("${external.fishing-index.timeout-ms:3000}")
  private long fishingIndexTimeoutMs;

  @Value("${external.fish-classify.connect-timeout-ms:1000}")
  private long classifyConnectTimeoutMs;

  @Value("${external.fish-classify.read-timeout-ms:5000}")
  private long classifyReadTimeoutMs;

  @Bean
  public RestClient fishingIndexRestClient() {
    return RestClient.builder()
        .requestFactory(requestFactory(fishingIndexTimeoutMs, fishingIndexTimeoutMs))
        .build();
  }

  /**
   * 어종 분류 모델 서버 전용. 모델 평균 응답이 80ms라 read timeout 5s는 순수 여유분이고, connect 1s는 모델 서버가 죽었을 때 사용자를 오래 붙잡지
   * 않기 위한 값이다(→ "목록에서 직접 선택" 대안 경로로 빨리 떨어뜨린다).
   */
  @Bean
  public RestClient fishClassifyRestClient() {
    return RestClient.builder()
        .requestFactory(requestFactory(classifyConnectTimeoutMs, classifyReadTimeoutMs))
        .build();
  }

  private SimpleClientHttpRequestFactory requestFactory(long connectMs, long readMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(connectMs));
    factory.setReadTimeout(Duration.ofMillis(readMs));
    return factory;
  }
}
