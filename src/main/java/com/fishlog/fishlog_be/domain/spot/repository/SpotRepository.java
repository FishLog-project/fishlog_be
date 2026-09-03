package com.fishlog.fishlog_be.domain.spot.repository;

import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpotRepository extends JpaRepository<Spot, Long> {

  /** 위치명으로 조회(시드 upsert 기준 키). */
  Optional<Spot> findByName(String name);

  boolean existsByName(String name);

  /**
   * 조회수 원자적 증가. 엔티티 로드 없이 {@code UPDATE ... +1}로 동시성 안전하게 올린다(read-modify-write 경합 방지). 호출은
   * {@code @Transactional} 컨텍스트에서만 유효(@Modifying).
   */
  @Modifying
  @Query("UPDATE Spot s SET s.viewCount = s.viewCount + 1 WHERE s.id = :id")
  void incrementViewCount(@Param("id") Long id);
}
