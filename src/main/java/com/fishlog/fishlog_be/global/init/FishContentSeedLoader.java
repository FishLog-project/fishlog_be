package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
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
 * <p>{@link SpotSeedLoader}와 분리된 이유: 스팟 시드는 "이미 적재됐으면 통째로 건너뛴다"는 count 가드가 있어 거기에 얹으면 기존 DB에는 콘텐츠가
 * 영영 반영되지 않는다. 이 로더는 가드와 무관하게 매번 돈다. 또한 스팟에 엮이지 않는 민물 어종(강·저수지·하천)은 {@link SpotSeedLoader}가 만들지
 * 않으므로, 여기서 직접 생성해야 도감에 등장한다.
 *
 * <p><b>적용 정책 — 항상 덮어쓰기 + 시드 동기화:</b> JSON이 도감 카탈로그의 단일 진실 공급원(source of truth)이다. 기동할 때마다 시드 값으로
 * 덮어쓰므로 JSON을 고치고 재시작하면 곧바로 반영된다. 따라서 <b>DB에서 직접 수정한 콘텐츠는 다음 기동에 사라진다</b>. 관리자 편집 기능이 생기면 이 정책을
 * 재검토해야 한다.
 *
 * <p><b>논리 삭제(비활성):</b> 시드에서 빠진 어종은 물리 삭제하지 않고 {@code isCollectible=false}로 내려 도감 조회·완성도 랭킹 집계에서만
 * 제외한다. 사용자가 이미 인증한 어종({@code catch_record} 참조)의 행과 기록을 보존하기 위함이다. 반대로 이전에 빠졌던 어종을 다시 시드에 넣으면 수집
 * 대상으로 복구된다.
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

    // 논리 삭제: 콘텐츠 시드는 도감 카탈로그의 단일 진실 공급원이므로, 시드에서 빠진 어종은 수집 대상에서 내려
    // 도감 조회·완성도 랭킹 집계에서 제외한다. 행과 사용자 인증 기록(catch_record)은 보존한다(물리 삭제 X).
    // 기타어종·placeholder 등 애초에 비수집 종은 시드에 없어도 이미 false 라 이 스캔이 건드리지 않는다.
    int deactivated = 0;
    for (Fish fish : fishRepository.findByIsCollectibleTrueOrderByIdAsc()) {
      if (!seedNames.contains(fish.getName())) {
        fish.markNotCollectible();
        deactivated++;
      }
    }
    log.info(
        "[seed] 어종 콘텐츠: 총 {}건 (신규 {}건 / 갱신 {}건 / 논리삭제 {}건)",
        data.fishes().size(),
        created,
        updated,
        deactivated);
  }

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
