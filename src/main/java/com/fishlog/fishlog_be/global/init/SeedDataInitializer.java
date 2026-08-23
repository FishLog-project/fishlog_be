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
 * SpotSeedLoader}(@Transactional)가 수행하며, 매 기동마다 돌되 name 기준 idempotent 라 중복이 생기지 않는다.
 *
 * <p>이어서 {@link FishContentSeedLoader}가 어종 도감 콘텐츠(설명·서식지)를 채운다. 어종 행이 먼저 존재해야 하므로 스팟 시드 <b>다음</b>에
 * 실행한다.
 *
 * <p>마지막으로 {@link InlandDetailSeedLoader}가 내륙(담수) 스팟의 하천 제원(하폭·유수폭·수심)을 채운다. 스팟 행을 참조하므로 역시 스팟 시드
 * 다음이다.
 */
@Component
@ConditionalOnProperty(name = "fishlog.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer {

  private final SpotSeedLoader spotSeedLoader;
  private final FishContentSeedLoader fishContentSeedLoader;
  private final InlandDetailSeedLoader inlandDetailSeedLoader;

  @PostConstruct
  public void init() {
    log.info("[seed] 낚시 스팟 시드 적재 시작");
    spotSeedLoader.load();
    log.info("[seed] 낚시 스팟 시드 적재 완료");

    log.info("[seed] 어종 도감 콘텐츠 적재 시작");
    fishContentSeedLoader.load();
    log.info("[seed] 어종 도감 콘텐츠 적재 완료");

    log.info("[seed] 담수 스팟 상세 적재 시작");
    inlandDetailSeedLoader.load();
    log.info("[seed] 담수 스팟 상세 적재 완료");
  }
}
