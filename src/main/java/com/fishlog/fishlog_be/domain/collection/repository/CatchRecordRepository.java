package com.fishlog.fishlog_be.domain.collection.repository;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CatchRecordRepository extends JpaRepository<CatchRecord, Long> {

  /**
   * 특정 사용자가 특정 어종을 인증한 기록 전체.
   *
   * <p>옵션 B: 잡은 횟수 = 반환 리스트 크기, 사진 목록 = 각 행의 certifiedImageUrl. {@code fish}는 연관관계라 프로퍼티 경로 {@code
   * Fish_Id}로 탐색한다(=fish.id).
   */
  List<CatchRecord> findByUserIdAndFish_Id(Long userId, Long fishId);

  /**
   * 완성도 랭킹: 사용자별 고유 수집대상 어종 수를 내림차순으로 집계한다.
   *
   * <p>같은 어종을 여러 번 인증하면 여러 행이므로 {@code COUNT(DISTINCT fish.id)}로 세고, 비수집 종({@code
   * is_collectible=false})은 도감 분모에 없으므로 분자에서도 제외한다. → docs/ranking.md
   */
  @Query(
      "SELECT c.userId AS userId, COUNT(DISTINCT c.fish.id) AS fishCount "
          + "FROM CatchRecord c "
          + "WHERE c.fish.isCollectible = true "
          + "GROUP BY c.userId "
          + "ORDER BY COUNT(DISTINCT c.fish.id) DESC")
  List<UserFishCount> findCompletionScores();

  /**
   * 크기 랭킹: 사용자별 최대 어종 크기(cm)를 내림차순으로 집계한다.
   *
   * <p>{@code size}는 NOT NULL이라 인증 기록이 있는 사용자만 결과에 포함된다. → docs/ranking.md
   */
  @Query(
      "SELECT c.userId AS userId, MAX(c.size) AS maxSize "
          + "FROM CatchRecord c "
          + "GROUP BY c.userId "
          + "ORDER BY MAX(c.size) DESC")
  List<UserMaxSize> findMaxSizeScores();
}
