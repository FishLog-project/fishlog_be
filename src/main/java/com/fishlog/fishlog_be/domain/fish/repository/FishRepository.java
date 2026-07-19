package com.fishlog.fishlog_be.domain.fish.repository;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FishRepository extends JpaRepository<Fish, Long> {

  /** 어종명으로 조회((스팟,어종) 페어 시드 매핑 기준 키). */
  Optional<Fish> findByName(String name);

  boolean existsByName(String name);

  /** 전체 도감 목록: 수집 대상 어종만 id 오름차순으로 조회. */
  List<Fish> findByIsCollectibleTrueOrderByIdAsc();

  /** 전체 도감 어종 수(완성도 랭킹 분모). 수집 대상 어종만 센다. → docs/ranking.md */
  long countByIsCollectibleTrue();

  /** 어종 상세: 수집 대상 어종을 id로 단건 조회(비수집 종은 조회되지 않음). */
  Optional<Fish> findByIdAndIsCollectibleTrue(Long id);

  /** 이름 완전일치 검색: 수집 대상 어종을 이름으로 단건 조회(비수집 종은 조회되지 않음). */
  Optional<Fish> findByNameAndIsCollectibleTrue(String name);
}
