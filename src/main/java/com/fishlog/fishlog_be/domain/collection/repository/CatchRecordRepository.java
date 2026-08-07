package com.fishlog.fishlog_be.domain.collection.repository;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatchRecordRepository extends JpaRepository<CatchRecord, Long> {

  /**
   * 특정 사용자가 특정 어종을 인증한 기록 전체.
   *
   * <p>옵션 B: 잡은 횟수 = 반환 리스트 크기, 사진 목록 = 각 행의 certifiedImageUrl. {@code fish}는 연관관계라 프로퍼티 경로 {@code
   * Fish_Id}로 탐색한다(=fish.id).
   */
  List<CatchRecord> findByUserIdAndFish_Id(Long userId, Long fishId);

  /**
   * 특정 어종에 대한 인증 기록 존재 여부.
   *
   * <p>시드에서 빠진 어종을 물리 삭제해도 되는지 판단하는 가드다. 기록이 하나라도 있으면 사용자 데이터가 사라지므로 삭제하지 않고 논리 삭제로 남긴다. → {@code
   * FishContentSeedLoader}
   */
  boolean existsByFish_Id(Long fishId);

  /**
   * 특정 사용자가 한 번이라도 인증한 어종의 id 집합(중복 제거).
   *
   * <p>내 도감 그리드에서 각 칸의 잡음/못잡음을 O(1)로 판정하기 위한 소스다. 같은 어종을 여러 번 잡아도 id 하나로 접힌다. → docs/ranking.md
   */
  @Query("SELECT DISTINCT c.fish.id FROM CatchRecord c WHERE c.userId = :userId")
  List<Long> findDistinctCaughtFishIds(@Param("userId") Long userId);

  /**
   * 회원탈퇴 시 해당 사용자의 모든 인증 기록을 삭제한다(하드).
   *
   * <p>{@code user_id}는 FK가 아니라 plain Long이라 DB 캐스케이드가 없다. 사용자 삭제 시 남으면 랭킹 집계에 유령 userId로 잡히므로 함께
   * 지운다. → docs/auth-followup.md
   */
  void deleteByUserId(Long userId);

  /**
   * 완성도 랭킹: 사용자별 고유 어종 수를 내림차순으로 집계한다.
   *
   * <p>같은 어종을 여러 번 인증하면 여러 행이므로 {@code COUNT(DISTINCT fish.id)}로 센다. 분모({@code fishes} 전체 수)와 같은
   * 집합을 세므로 완성도가 100%를 넘지 않는다. → docs/ranking.md
   */
  @Query(
      "SELECT c.userId AS userId, COUNT(DISTINCT c.fish.id) AS fishCount "
          + "FROM CatchRecord c "
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
