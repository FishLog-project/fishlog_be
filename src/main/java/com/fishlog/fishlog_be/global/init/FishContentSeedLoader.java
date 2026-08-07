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
 * <p><b>정리(prune) — 물리 삭제 우선, 인증 기록이 있으면 논리 삭제:</b> 시드에서 빠진 어종은 DB에서도 지워 {@code fishes} 테이블이 시드와
 * 정확히 일치하게 만든다. 삭제 전 {@code major_fish} 참조를 먼저 끊는다.
 *
 * <p>단 <b>{@code catch_record}(사용자 인증 기록)가 하나라도 있으면 삭제하지 않고</b> {@code isCollectible=false}로만 내린다.
 * 어종 행을 지우면 사용자가 인증한 기록까지 함께 사라지기 때문이다. 이 경우 WARN 로그로 남기며, 해당 어종은 도감·랭킹에서만 빠지고 행은 유지된다.
 *
 * <p>이전에 논리 삭제됐던 어종을 다시 시드에 넣으면 수집 대상으로 복구된다.
 *
 * <p>새로 만드는 어종은 {@code isCollectible=true}(도감 노출)로 생성한다. {@code rarity}는 대소문자를 가리지 않고 파싱하며, 값이 없거나
 * 알 수 없으면 null 로 둔다. → docs/spec.md
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
        fish = fishRepository.save(Fish.builder().name(seed.name()).isCollectible(true).build());
        created++;
      } else {
        // 기존 어종은 영속 상태라 커밋 시 dirty checking 으로 UPDATE 된다(값이 같으면 Hibernate 가 생략).
        // 이전에 논리 삭제(비활성)됐다가 다시 시드에 추가된 경우를 대비해 수집 대상으로 복구한다.
        fish.markCollectible();
        updated++;
      }
      // 신규·기존 공통으로 콘텐츠(설명·서식지·희귀도)를 덮어쓴다.
      fish.applyContent(seed.description(), seed.habitat(), rarity);
    }

    Pruned pruned = prune(seedNames);

    log.info(
        "[seed] 어종 콘텐츠: 총 {}건 (신규 {}건 / 갱신 {}건 / 삭제 {}건 / 논리삭제 {}건)",
        data.fishes().size(),
        created,
        updated,
        pruned.deleted(),
        pruned.deactivated());
  }

  /**
   * 콘텐츠 시드에 없는 어종을 정리한다. 시드가 도감 카탈로그의 단일 진실 공급원이므로 {@code fishes}를 시드와 일치시킨다.
   *
   * <p>인증 기록({@code catch_record})이 없으면 {@code major_fish} 참조를 끊고 <b>물리 삭제</b>, 있으면 사용자 데이터 보호를 위해
   * <b>논리 삭제</b>(WARN)로 남긴다. 비수집 종({@code isCollectible=false})도 시드에 없으면 정리 대상이라 전체를 스캔한다.
   */
  private Pruned prune(Set<String> seedNames) {
    int deleted = 0;
    int deactivated = 0;
    for (Fish fish : fishRepository.findAll()) {
      if (seedNames.contains(fish.getName())) {
        continue;
      }
      if (catchRecordRepository.existsByFish_Id(fish.getId())) {
        // 사용자 인증 기록이 걸려 있어 지울 수 없다. 도감·랭킹에서만 제외하고 행은 보존한다.
        if (fish.isCollectible()) {
          fish.markNotCollectible();
        }
        log.warn("[seed] 시드에서 빠졌으나 인증 기록이 있어 논리 삭제로 유지: {}(id={})", fish.getName(), fish.getId());
        deactivated++;
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
    return new Pruned(deleted, deactivated);
  }

  /** 정리 결과 집계(물리 삭제 건수 / 인증 기록 때문에 논리 삭제로 남긴 건수). */
  private record Pruned(int deleted, int deactivated) {}

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
}
