package com.fishlog.fishlog_be.global.init;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 낚시 스팟 시드 데이터를 DB에 적재한다. (#13)
 *
 * <p>{@code fishlog.seed.enabled=true} 일 때만 빈이 등록되어 동작한다(기본 비활성). {@link PostConstruct}로 **컨텍스트
 * 초기화(refresh) 단계**에 실행되므로, 웹서버(Tomcat)가 요청을 받기 전에 적재가 끝난다. 실제 upsert 는 {@link
 * SpotSeedLoader}(@Transactional)가 수행하며, 이미 적재돼 있으면 건너뛴다.
 *
 * <p>이어서 {@link FishContentSeedLoader}가 어종 도감 콘텐츠(설명·서식지)를 채운다. 어종 행이 먼저 존재해야 하므로 스팟 시드 <b>다음</b>에
 * 실행한다.
 */
@Component
@ConditionalOnProperty(name = "fishlog.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer {

  private final SpotSeedLoader spotSeedLoader;
  private final FishContentSeedLoader fishContentSeedLoader;

  @PostConstruct
  public void init() {
    log.info("[seed] 낚시 스팟 시드 적재 시작");
    spotSeedLoader.load();
    log.info("[seed] 낚시 스팟 시드 적재 완료");

    log.info("[seed] 어종 도감 콘텐츠 적재 시작");
    fishContentSeedLoader.load();
    log.info("[seed] 어종 도감 콘텐츠 적재 완료");
  }
}
