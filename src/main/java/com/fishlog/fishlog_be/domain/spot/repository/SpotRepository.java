package com.fishlog.fishlog_be.domain.spot.repository;

import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotRepository extends JpaRepository<Spot, Long> {

  /** 위치명으로 조회(시드 upsert 기준 키). */
  Optional<Spot> findByName(String name);

  boolean existsByName(String name);
}
