package com.fishlog.fishlog_be.domain.spot.repository;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.spot.entity.FishSopt;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FishSoptRepository extends JpaRepository<FishSopt, Long> {

  /** 스팟의 주요 대상 어종 매핑 목록(상세 조회용). */
  List<FishSopt> findBySpot(Spot spot);

  /** (스팟,어종) 페어 존재 여부(시드 idempotent upsert 용). */
  boolean existsBySpotAndFish(Spot spot, Fish fish);
}
