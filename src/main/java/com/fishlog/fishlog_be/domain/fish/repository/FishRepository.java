package com.fishlog.fishlog_be.domain.fish.repository;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FishRepository extends JpaRepository<Fish, Long> {

  /** 어종명으로 조회((스팟,어종) 페어 시드 매핑 기준 키). */
  Optional<Fish> findByName(String name);

  boolean existsByName(String name);
}
