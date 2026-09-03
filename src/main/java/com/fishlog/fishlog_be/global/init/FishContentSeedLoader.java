package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.domain.spot.repository.MajorFishRepository;
import com.fishlog.fishlog_be.global.init.dto.FishContentSeed;
import com.fishlog.fishlog_be.global.init.dto.FishContentSeedData;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어종 도감 콘텐츠(설명·서식지·희귀도)를 적재한다. 이름({@code fishes.name} UNIQUE) 기준 <b>upsert</b>다: DB에 이미 있으면 콘텐츠를
 * 덮어쓰고(update), 없으면 새 어종으로 만든다(insert).
 *
 * <p>{@link SpotSeedLoader}와 분리된 이유: 스팟 매핑(major_fish)과 도감 콘텐츠는 갱신 주기와 출처가 다르다. 스팟에 엮이지 않는 어종도 여기서
 * 직접 생성해야 도감에 등장한다(현재 시드는 24종 전부 스팟에 매핑돼 있지만, 콘텐츠 시드가 도감 카탈로그의 단일 진실 공급원인 구조는 유지한다).
 *
 * <p><b>적용 정책 — 항상 덮어쓰기 + 시드 동기화:</b> JSON이 도감 카탈로그의 단일 진실 공급원(source of truth)이다. 기동할 때마다 시드 값으로
 * 덮어쓰므로 JSON을 고치고 재시작하면 곧바로 반영된다. 따라서 <b>DB에서 직접 수정한 콘텐츠는 다음 기동에 사라진다</b>. 관리자 편집 기능이 생기면 이 정책을
 * 재검토해야 한다.
 *
 * <p><b>정리(prune) — 시드에 없으면 삭제:</b> 시드에서 빠진 어종은 DB에서도 지워 {@code fishes} 테이블이 시드와 정확히 일치하게 만든다. 삭제 전
 * {@code major_fish} 참조를 먼저 끊는다. 따라서 <b>{@code fishes}의 모든 행이 곧 전체 도감</b>이며, "도감에서 숨긴 어종"이라는 상태는
 * 없다.
 *
 * <p><b>⚠️ 인증 기록 가드:</b> {@code catch_record}(사용자 인증 기록)가 하나라도 있는 어종은 <b>삭제하지 않고 건너뛴다</b>(WARN). 어종
 * 행을 지우면 사용자가 인증한 기록까지 함께 사라지기 때문이다. 이 경우 그 어종은 시드에 없는데도 도감에 남으므로, WARN 을 보면 시드를 되돌리거나 기록 이관을 결정해야
 * 한다. 확정 24종에서는 발생하지 않는 경로다.
 *
 * <p>{@code rarity}는 대소문자를 가리지 않고 파싱하며, 값이 없거나 알 수 없으면 null 로 둔다. → docs/spec.md
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FishContentSeedLoader {

  private final SeedDataReader seedDataReader;
  private final FishRepository fishRepository;
  private final CatchRecordRepository catchRecordRepository;
  private final MajorFishRepository majorFishRepository;

  @Transactional
  public void load() {
    FishContentSeedData data = seedDataReader.readFishContents();
    Set<String> seedNames = new HashSet<>();
    int created = 0;
    int updated = 0;
    for (FishContentSeed seed : data.fishes()) {
      seedNames.add(seed.name());
      Rarity rarity = parseRarity(seed.rarity());
      Fish fish = fishRepository.findByName(seed.name()).orElse(null);
      if (fish == null) {
        // 시드에 있으나 DB에 없는 어종 → 새로 생성해 persist. 반환된 관리 엔티티를 담아야
        // 아래 applyContent 의 dirty checking 이 이 행에 반영된다.
        fish = fishRepository.save(Fish.builder().name(seed.name()).build());
        created++;
      } else {
        // 기존 어종은 영속 상태라 커밋 시 dirty checking 으로 UPDATE 된다(값이 같으면 Hibernate 가 생략).
        updated++;
      }
      // 신규·기존 공통으로 콘텐츠(설명·서식지·희귀도)를 덮어쓴다.
      fish.applyContent(seed.description(), seed.habitat(), rarity);
      applySeasons(fish, seed);
    }

    Pruned pruned = prune(seedNames);

    log.info(
        "[seed] 어종 콘텐츠: 총 {}건 (신규 {}건 / 갱신 {}건 / 삭제 {}건 / 삭제보류 {}건)",
        data.fishes().size(),
        created,
        updated,
        pruned.deleted(),
        pruned.kept());
  }

  /**
   * 콘텐츠 시드에 없는 어종을 정리한다. 시드가 도감 카탈로그의 단일 진실 공급원이므로 {@code fishes}를 시드와 일치시킨다.
   *
   * <p>인증 기록({@code catch_record})이 없으면 {@code major_fish} 참조를 끊고 삭제한다. 기록이 있으면 사용자 데이터가 함께 사라지므로
   * <b>삭제를 보류</b>하고 WARN 만 남긴다(그 어종은 시드에 없는데도 도감에 남는다).
   */
  private Pruned prune(Set<String> seedNames) {
    int deleted = 0;
    int kept = 0;
    for (Fish fish : fishRepository.findAll()) {
      if (seedNames.contains(fish.getName())) {
        continue;
      }
      if (catchRecordRepository.existsByFish_Id(fish.getId())) {
        // 사용자 인증 기록이 걸려 있어 지울 수 없다. 삭제를 보류하고 도감에 그대로 남긴다.
        log.warn(
            "[seed] 시드에서 빠졌으나 인증 기록이 있어 삭제 보류(도감에 그대로 남음): {}(id={}). "
                + "시드를 되돌리거나 기록 이관 여부를 결정해야 한다.",
            fish.getName(),
            fish.getId());
        kept++;
        continue;
      }
      // FK 참조를 먼저 끊어야 fishes 행을 지울 수 있다.
      long unmapped = majorFishRepository.deleteByFish(fish);
      fishRepository.delete(fish);
      deleted++;
      if (unmapped > 0) {
        // 스팟 시드(spot_fish_seed.json)는 이 어종을 아직 참조하는데 콘텐츠 시드에는 없다는 뜻이다.
        // 두 시드가 어긋난 상태라 매 기동마다 생성→삭제가 반복되므로 시드 파일을 맞춰야 한다.
        log.warn(
            "[seed] 어종 삭제: {}(id={}) — 스팟 매핑 {}건도 함께 삭제됨. "
                + "spot_fish_seed.json 과 fish_content_seed.json 의 어종 목록이 어긋나 있는지 확인 필요.",
            fish.getName(),
            fish.getId(),
            unmapped);
      } else {
        log.info("[seed] 시드에서 빠진 어종 삭제: {}(id={})", fish.getName(), fish.getId());
      }
    }
    return new Pruned(deleted, kept);
  }

  /** 정리 결과 집계(삭제 건수 / 인증 기록 때문에 삭제를 보류한 건수). */
  private record Pruned(int deleted, int kept) {}

  /** 시드의 rarity 문자열(대소문자 무관)을 {@link Rarity}로 변환한다. 비었거나 알 수 없는 값이면 null. */
  private Rarity parseRarity(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Rarity.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      log.warn("[seed] 알 수 없는 rarity 값 → null 처리: {}", raw);
      return null;
    }
  }

  /** 시드의 {@code seasons} 목록("봄/여름/가을/겨울")을 계절별 boolean으로 매핑해 어종에 적용한다. 없으면 전부 false. */
  private void applySeasons(Fish fish, FishContentSeed seed) {
    Set<String> seasons =
        seed.seasons() == null
            ? Set.of()
            : new HashSet<>(seed.seasons().stream().map(String::trim).toList());
    fish.applySeasons(
        seasons.contains("봄"),
        seasons.contains("여름"),
        seasons.contains("가을"),
        seasons.contains("겨울"));
  }
}
