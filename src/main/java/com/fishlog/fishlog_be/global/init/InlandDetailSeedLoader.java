package com.fishlog.fishlog_be.global.init;

import com.fishlog.fishlog_be.domain.spot.entity.InlandSpotDetail;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import com.fishlog.fishlog_be.domain.spot.repository.InlandSpotDetailRepository;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import com.fishlog.fishlog_be.global.init.dto.InlandDetailSeed;
import com.fishlog.fishlog_be.global.init.dto.InlandDetailSeedData;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내륙(담수) 스팟의 하천 제원(하폭·유수폭·수심)을 적재한다. 스팟 이름({@code spots.name} UNIQUE) 기준 <b>upsert</b> 이며, 스팟 행이 먼저
 * 있어야 하므로 {@link SpotSeedLoader} <b>다음</b>에 실행한다.
 *
 * <p><b>적용 정책 — 항상 덮어쓰기:</b> {@code data/spot/inland_detail_seed.json}이 단일 진실 공급원이라 기동할 때마다 시드 값으로
 * 덮어쓴다({@link FishContentSeedLoader}와 같은 정책).
 *
 * <p><b>정리(prune):</b> 시드에 없는 스팟의 상세는 삭제한다. 스팟 자체가 시드에서 빠진 경우는 {@link SpotSeedLoader}가 스팟과 함께 이미
 * 지우므로, 여기 정리 단계가 잡는 건 <b>스팟은 남았는데 상세만 빠진 경우</b>(예: 해양으로 재분류)다.
 *
 * <p>어종 정리와 달리 보류 조건이 없다 — 이 테이블을 참조하는 곳도, 사용자가 만든 데이터도 없다. → docs/spec.md "담수 스팟 상세"
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InlandDetailSeedLoader {

  private final SeedDataReader seedDataReader;
  private final SpotRepository spotRepository;
  private final InlandSpotDetailRepository inlandSpotDetailRepository;

  @Transactional
  public void load() {
    InlandDetailSeedData data = seedDataReader.readInlandDetails();
    Set<Long> seedSpotIds = new HashSet<>();
    int created = 0;
    int updated = 0;
    int skipped = 0;

    for (InlandDetailSeed seed : data.details()) {
      Spot spot = spotRepository.findByName(seed.spot()).orElse(null);
      if (spot == null) {
        // 스팟 시드와 상세 시드가 어긋났다는 뜻이다(둘 다 build_seed.py 산출물이라 정상적으론 발생하지 않는다).
        log.warn("[seed] 담수 상세 스킵(스팟 없음): spot={}", seed.spot());
        skipped++;
        continue;
      }
      if (spot.getCategory() != SpotCategory.내륙) {
        log.warn(
            "[seed] 담수 상세 스킵(내륙 스팟이 아님): spot={}, category={}", seed.spot(), spot.getCategory());
        skipped++;
        continue;
      }
      seedSpotIds.add(spot.getId());

      InlandSpotDetail detail = inlandSpotDetailRepository.findBySpot(spot).orElse(null);
      if (detail == null) {
        inlandSpotDetailRepository.save(
            InlandSpotDetail.builder()
                .spot(spot)
                .riverWidthMin(seed.riverWidthMin())
                .riverWidthMax(seed.riverWidthMax())
                .flowWidthMin(seed.flowWidthMin())
                .flowWidthMax(seed.flowWidthMax())
                .depthMin(seed.depthMin())
                .depthMax(seed.depthMax())
                .build());
        created++;
      } else {
        // 영속 상태라 커밋 시 dirty checking 으로 UPDATE 된다(값이 같으면 Hibernate 가 생략).
        detail.applyMeasures(
            seed.riverWidthMin(),
            seed.riverWidthMax(),
            seed.flowWidthMin(),
            seed.flowWidthMax(),
            seed.depthMin(),
            seed.depthMax());
        updated++;
      }
    }

    int deleted = prune(seedSpotIds);
    log.info(
        "[seed] 담수 스팟 상세: 총 {}건 (신규 {}건 / 갱신 {}건 / 삭제 {}건 / 스킵 {}건)",
        data.details().size(),
        created,
        updated,
        deleted,
        skipped);
  }

  /** 시드에 없는 상세를 삭제한다. 스팟 id 로 비교해 LAZY 프록시를 건드리지 않는다(N+1 방지). */
  private int prune(Set<Long> seedSpotIds) {
    int deleted = 0;
    for (InlandSpotDetail detail : inlandSpotDetailRepository.findAll()) {
      if (seedSpotIds.contains(detail.getSpotId())) {
        continue;
      }
      inlandSpotDetailRepository.delete(detail);
      deleted++;
      log.info("[seed] 시드에서 빠진 담수 상세 삭제: spotId={}", detail.getSpotId());
    }
    return deleted;
  }
}
