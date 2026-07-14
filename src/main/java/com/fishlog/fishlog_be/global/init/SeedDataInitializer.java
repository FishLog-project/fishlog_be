package com.fishlog.fishlog_be.global.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 낚시 스팟 시드 데이터를 DB에 적재한다. (#13)
 *
 * <p>{@code fishlog.seed.enabled=true} 일 때만 동작한다(기본 비활성). 실제 upsert 는 {@link SpotSeedLoader}가 트랜잭션
 * 안에서 수행하며, 재실행해도 중복이 생기지 않는다.
 */
@Component
@ConditionalOnProperty(name = "fishlog.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer implements ApplicationRunner {

  private final SpotSeedLoader spotSeedLoader;

  @Override
  public void run(ApplicationArguments args) {
    log.info("[seed] 낚시 스팟 시드 적재 시작");
    spotSeedLoader.load();
    log.info("[seed] 낚시 스팟 시드 적재 완료");
  }
}
