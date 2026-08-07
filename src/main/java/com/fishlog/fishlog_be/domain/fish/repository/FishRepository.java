package com.fishlog.fishlog_be.domain.fish.repository;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FishRepository extends JpaRepository<Fish, Long> {

  /** 어종명으로 조회((스팟,어종) 페어 시드 매핑 기준 키). */
  Optional<Fish> findByName(String name);

  boolean existsByName(String name);

  /**
   * 전체 도감 목록: id 오름차순 조회.
   *
   * <p>{@code fishes}의 모든 행이 곧 도감이라 별도 필터가 없다(시드에 없는 어종은 로더가 삭제한다). 어종 수(완성도 랭킹 분모)는 {@code
   * count()}, 단건 조회는 {@code findById()}·{@link #findByName(String)}를 그대로 쓴다. → docs/ranking.md
   */
  List<Fish> findAllByOrderByIdAsc();
}
