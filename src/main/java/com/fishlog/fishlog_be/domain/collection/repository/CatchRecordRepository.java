package com.fishlog.fishlog_be.domain.collection.repository;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatchRecordRepository extends JpaRepository<CatchRecord, Long> {

  /**
   * 특정 사용자가 특정 어종을 인증한 기록 전체.
   *
   * <p>옵션 B: 잡은 횟수 = 반환 리스트 크기, 사진 목록 = 각 행의 certifiedImageUrl. {@code fish}는 연관관계라 프로퍼티 경로 {@code
   * Fish_Id}로 탐색한다(=fish.id).
   */
  List<CatchRecord> findByUserIdAndFish_Id(Long userId, Long fishId);
}
