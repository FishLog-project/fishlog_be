package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.domain.spot.entity.MajorFish;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import com.fishlog.fishlog_be.domain.spot.repository.MajorFishRepository;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import com.fishlog.fishlog_be.global.init.dto.SpotFishSeedData;
import com.fishlog.fishlog_be.global.init.dto.SpotSeed;
import com.fishlog.fishlog_be.global.init.dto.SpotSeedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
 *   <li>정리 : 시드에 없는 스팟은 매핑과 함께 삭제(이름 변경 시 고아 행이 남지 않도록).
 * </ul>
 *
 * 재실행해도 중복이 생기지 않는다.
 *
 * <p><b>어종 카탈로그의 최종 기준은 {@link FishContentSeedLoader}다.</b> 여기서 만든 어종이라도 콘텐츠 시드({@code
 * data/fish/fish_content_seed.json})에 없으면 뒤이어 정리(물리 삭제)된다. 따라서 두 시드의 어종 목록은 일치해야 하며, 어긋나면 매 기동마다
 * 생성·삭제가 반복되고 정리 단계에서 WARN 이 남는다. → docs/spec.md "스팟 데이터 설계", docs/external.md §1
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
    // 매 기동마다 돈다. 예전에는 "spots 가 1건이라도 있으면 통째로 건너뛴다"는 가드가 있었으나,
    // 그러면 이미 적재된 DB에 시드가 늘어나도(49곳 → 95곳) 새 스팟이 영영 반영되지 않는다.
    // 아래 upsert 는 name 기준 idempotent 라 재실행해도 중복이 생기지 않는다.
    Map<String, Spot> spotByName = upsertSpots();
    upsertMajorFishes(spotByName);
    pruneSpots(spotByName.keySet());
  }

  /**
   * 시드에 없는 스팟을 삭제한다. 시드 JSON이 스팟 목록의 단일 진실 공급원이므로 {@code spots}를 시드와 일치시킨다.
   *
   * <p>스팟 이름이 바뀌면(예: 중복 분리로 {@code 위천} → {@code 위천(1)}·{@code 위천(2)}) upsert 는 새 이름을 만들 뿐 옛 행을 지우지
   * 않아 고아 스팟이 남는다. 이를 막기 위한 정리 단계다. {@code spots}를 참조하는 것은 {@code major_fish} 뿐이라(사용자 데이터 없음) 어종
   * 정리와 달리 보류 조건이 없다.
   */
  private void pruneSpots(Set<String> seedNames) {
    int deleted = 0;
    for (Spot spot : spotRepository.findAll()) {
      if (seedNames.contains(spot.getName())) {
        continue;
      }
      long unmapped = majorFishRepository.deleteBySpot(spot);
      spotRepository.delete(spot);
      deleted++;
      log.info("[seed] 시드에서 빠진 스팟 삭제: {}(id={}, 매핑 {}건)", spot.getName(), spot.getId(), unmapped);
    }
    if (deleted > 0) {
      log.info("[seed] spots 정리: {}개 삭제", deleted);
    }
  }

  private Map<String, Spot> upsertSpots() {
    SpotSeedData data = seedDataReader.readSpots();
    Map<String, Spot> spotByName = new HashMap<>();
    int created = 0;
    for (SpotSeed s : data.spots()) {
      SpotCategory category = SpotCategory.fromLabel(s.category());
      Spot existing = spotRepository.findByName(s.name()).orElse(null);
      if (existing != null) {
        // 기존 행은 유지(운영값 prohibit 보존)하되, category는 backfill/갱신.
        if (existing.getCategory() != category) {
          existing.applyCategory(category);
        }
        spotByName.put(s.name(), existing);
        continue;
      }
      Spot saved =
          spotRepository.save(
              Spot.builder()
                  .name(s.name())
                  .lat(s.lat())
                  .lot(s.lot())
                  .prohibit(false)
                  .category(category)
                  .build());
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
