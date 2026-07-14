package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.domain.spot.entity.MajorFish;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.repository.MajorFishRepository;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import com.fishlog.fishlog_be.global.init.dto.SpotFishSeedData;
import com.fishlog.fishlog_be.global.init.dto.SpotSeed;
import com.fishlog.fishlog_be.global.init.dto.SpotSeedData;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시드 JSON({@link SeedDataReader})을 DB에 **idempotent upsert** 한다. (#13)
 *
 * <ul>
 *   <li>{@code spots} : name UNIQUE 기준으로 없으면 생성(있으면 유지 — 운영값 {@code prohibit} 보존).
 *   <li>{@code fishes} : name UNIQUE 기준으로 없으면 생성.
 *   <li>{@code major_fish} : (spot, fish) 조합이 없을 때만 생성.
 * </ul>
 *
 * 재실행해도 중복이 생기지 않는다. → docs/spec.md "스팟 데이터 설계", docs/external.md §1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpotSeedLoader {

  private final SeedDataReader seedDataReader;
  private final SpotRepository spotRepository;
  private final FishRepository fishRepository;
  private final MajorFishRepository majorFishRepository;

  @Transactional
  public void load() {
    // 안전장치: 이미 적재돼 있으면 건너뛴다("1회만" 운용을 코드로 보장).
    // 재적재는 어차피 idempotent(신규 0건)지만, 불필요한 전체 조회를 막는다.
    long existing = spotRepository.count();
    if (existing > 0) {
      log.info("[seed] 이미 적재됨(spots={}개) → 건너뜀", existing);
      return;
    }
    Map<String, Spot> spotByName = upsertSpots();
    upsertMajorFishes(spotByName);
  }

  private Map<String, Spot> upsertSpots() {
    SpotSeedData data = seedDataReader.readSpots();
    Map<String, Spot> spotByName = new HashMap<>();
    int created = 0;
    for (SpotSeed s : data.spots()) {
      Spot existing = spotRepository.findByName(s.name()).orElse(null);
      if (existing != null) {
        spotByName.put(s.name(), existing);
        continue;
      }
      Spot saved =
          spotRepository.save(
              Spot.builder().name(s.name()).lat(s.lat()).lot(s.lot()).prohibit(false).build());
      spotByName.put(s.name(), saved);
      created++;
    }
    log.info("[seed] spots: {}개 중 신규 {}개 적재", data.spots().size(), created);
    return spotByName;
  }

  private void upsertMajorFishes(Map<String, Spot> spotByName) {
    SpotFishSeedData data = seedDataReader.readSpotFishes();

    // 어종 카탈로그 upsert (name UNIQUE)
    Map<String, Fish> fishByName = new HashMap<>();
    int fishCreated = 0;
    for (String name : data.fishes()) {
      Fish existing = fishRepository.findByName(name).orElse(null);
      if (existing == null) {
        existing = fishRepository.save(Fish.builder().name(name).build());
        fishCreated++;
      }
      fishByName.put(name, existing);
    }

    // (스팟, 어종) 매핑 upsert
    int pairCreated = 0;
    int skipped = 0;
    for (var pair : data.pairs()) {
      Spot spot = spotByName.get(pair.spot());
      Fish fish = fishByName.get(pair.fish());
      if (spot == null || fish == null) {
        log.warn("[seed] major_fish 매핑 스킵(미해결): spot={}, fish={}", pair.spot(), pair.fish());
        skipped++;
        continue;
      }
      if (majorFishRepository.existsBySpotAndFish(spot, fish)) {
        continue;
      }
      majorFishRepository.save(MajorFish.builder().spot(spot).fish(fish).build());
      pairCreated++;
    }
    log.info(
        "[seed] fishes: {}종 중 신규 {}종 / major_fish: 페어 {}개 중 신규 {}개(스킵 {}개)",
        data.fishes().size(),
        fishCreated,
        data.pairs().size(),
        pairCreated,
        skipped);
  }
}
