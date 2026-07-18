package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.global.init.dto.FishContentSeed;
import com.fishlog.fishlog_be.global.init.dto.FishContentSeedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어종 도감 콘텐츠(설명·서식지)를 기존 {@code fishes} 행에 채운다.
 *
 * <p>{@link SpotSeedLoader}와 분리된 이유: 스팟 시드는 "이미 적재됐으면 통째로 건너뛴다"는 count 가드가 있고 어종도 <b>없을 때만
 * insert</b> 하므로, 거기에 얹으면 기존 DB에는 콘텐츠가 영영 반영되지 않는다. 이 로더는 가드와 무관하게 매번 돌며 <b>기존 행을 update</b> 한다.
 *
 * <p><b>적용 정책 — 항상 덮어쓰기:</b> JSON이 도감 콘텐츠의 단일 진실 공급원(source of truth)이다. 기동할 때마다 시드 값으로 덮어쓰므로 JSON을
 * 고치고 재시작하면 곧바로 반영된다. 따라서 <b>DB에서 직접 수정한 콘텐츠는 다음 기동에 사라진다</b>. 관리자 편집 기능이 생기면 이 정책을 재검토해야 한다.
 *
 * <p>시드에 없는 어종({@code 기타어종} 등)이나 DB에 없는 이름은 건너뛴다. → docs/spec.md
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
    int updated = 0;
    int skipped = 0;
    for (FishContentSeed seed : data.fishes()) {
      Fish fish = fishRepository.findByName(seed.name()).orElse(null);
      if (fish == null) {
        log.warn("[seed] 어종 콘텐츠 스킵(DB에 없는 어종): {}", seed.name());
        skipped++;
        continue;
      }
      // 영속 상태이므로 트랜잭션 커밋 시 dirty checking 으로 UPDATE 된다(save 호출 불필요).
      // 값이 같으면 Hibernate 가 UPDATE 자체를 생략하므로 매 기동 덮어써도 부담이 없다.
      fish.applyContent(seed.description(), seed.habitat());
      updated++;
    }
    log.info("[seed] 어종 콘텐츠: {}건 중 {}건 반영(스킵 {}건)", data.fishes().size(), updated, skipped);
  }
}
