package com.fishlog.fishlog_be.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 HTTP 호출용 {@link RestClient} 설정. 바다낚시지수 예보 호출({@code global/forecast})에 쓰이며, 연결·읽기 타임아웃을 둬 외부
 * 지연이 상세 응답을 오래 붙잡지 않게 한다. → docs/external.md §1
 */
@Configuration
public class RestClientConfig {

  @Value("${external.fishing-index.timeout-ms:3000}")
  private long timeoutMs;

  @Bean
  public RestClient fishingIndexRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
    factory.setReadTimeout(Duration.ofMillis(timeoutMs));
    return RestClient.builder().requestFactory(factory).build();
  }
}
