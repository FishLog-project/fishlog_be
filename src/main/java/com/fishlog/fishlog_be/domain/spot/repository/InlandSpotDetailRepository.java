package com.fishlog.fishlog_be.domain.spot.repository;

import com.fishlog.fishlog_be.domain.spot.entity.InlandSpotDetail;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InlandSpotDetailRepository extends JpaRepository<InlandSpotDetail, Long> {

  /** 스팟의 하천 제원(상세 조회·시드 upsert 용). 내륙 스팟에만 존재한다. */
  Optional<InlandSpotDetail> findBySpot(Spot spot);

  /**
   * 특정 스팟의 하천 제원을 삭제하고 삭제 건수를 반환한다.
   *
   * <p>시드에서 빠진 스팟을 삭제하기 전 {@code inland_spot_detail.spot_id} FK 참조를 끊기 위해 쓴다. → {@code
   * SpotSeedLoader}
   */
  long deleteBySpot(Spot spot);
}
