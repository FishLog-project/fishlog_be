package com.fishlog.fishlog_be.domain.spot.repository;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.spot.entity.MajorFish;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MajorFishRepository extends JpaRepository<MajorFish, Long> {

  /** 스팟의 주요 대상 어종 매핑 목록(상세 조회용). */
  List<MajorFish> findBySpot(Spot spot);

  /** (스팟,어종) 페어 존재 여부(시드 idempotent upsert 용). */
  boolean existsBySpotAndFish(Spot spot, Fish fish);

  /**
   * 특정 어종을 참조하는 매핑을 모두 삭제하고 삭제 건수를 반환한다.
   *
   * <p>시드에서 빠진 어종을 물리 삭제하기 전, {@code major_fish.fishes_id} FK 참조를 먼저 끊기 위해 쓴다. 반환값이 0보다 크면 스팟 시드가
   * 아직 그 어종을 참조하고 있다는 뜻이라 호출부에서 경고한다. → {@code FishContentSeedLoader}
   */
  long deleteByFish(Fish fish);

  /**
   * 특정 스팟의 매핑을 모두 삭제하고 삭제 건수를 반환한다.
   *
   * <p>시드에서 빠진 스팟을 삭제하기 전 {@code major_fish.spots_id} FK 참조를 끊기 위해 쓴다. → {@code SpotSeedLoader}
   */
  long deleteBySpot(Spot spot);
}
