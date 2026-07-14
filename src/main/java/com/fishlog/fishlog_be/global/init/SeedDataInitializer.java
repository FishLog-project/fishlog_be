package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.global.init.dto.SpotFishSeedData;
import com.fishlog.fishlog_be.global.init.dto.SpotSeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 낚시 스팟 시드 데이터를 로드한다.
 *
 * <p>{@code fishlog.seed.enabled=true} 일 때만 동작한다(기본 비활성). 현재는 시드 파일을 읽어 건수만 검증·로깅하며, 실제 DB upsert 는
 * 엔티티/레포지토리(#12)가 준비되면 이 자리에 추가한다(#13).
 */
@Component
@ConditionalOnProperty(name = "fishlog.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SeedDataInitializer implements ApplicationRunner {

  private final SeedDataReader seedDataReader;

  @Override
  public void run(ApplicationArguments args) {
    SpotSeedData spots = seedDataReader.readSpots();
    SpotFishSeedData spotFishes = seedDataReader.readSpotFishes();

    log.info(
        "[seed] 스팟 {}개, 어종 {}종, (스팟,어종) 페어 {}개 로드 완료",
        spots.spots().size(),
        spotFishes.fishCount(),
        spotFishes.pairs().size());

    // TODO(#13): SpotRepository / FishRepository / FishSoptRepository 로 idempotent upsert.
    //  - spots.spots()      → spots (name UNIQUE 기준)
    //  - spotFishes.pairs() → fishes 생성/매핑 후 fish_sopt (spot,fish 조합 UNIQUE)
  }
}
