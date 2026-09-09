package com.fishlog.fishlog_be.domain.collection.repository;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatchRecordRepository extends JpaRepository<CatchRecord, Long> {

  /**
   * 특정 사용자가 특정 어종을 인증한 기록을 <b>최신순으로</b> 조회한다. 도감 상세가 최근 사진 몇 장만 보여주므로 전체를 가져오지 않고 {@code Pageable}로
   * 개수를 제한한다(사진이 수백 장이어도 응답 크기가 고정된다).
   *
   * <p>정렬에 {@code Id DESC}를 덧붙인 이유: {@code createdAt}이 같은 순간의 기록끼리는 순서가 DB 구현에 맡겨져, 새로고침할 때마다 사진
   * 순서가 뒤바뀔 수 있다. id 는 단조 증가라 동점을 결정적으로 깬다.
   *
   * <p>{@code fish}는 연관관계라 프로퍼티 경로 {@code Fish_Id}로 탐색한다(=fish.id).
   */
  List<CatchRecord> findByUserIdAndFish_IdOrderByCreatedAtDescIdDesc(
      Long userId, Long fishId, Pageable pageable);

  /**
   * 특정 사용자의 인증 기록 <b>전체</b>를 최신순으로, 어종을 함께 조회한다(기록 목록 화면용).
   *
   * <p>도감 상세 조회와 달리 개수를 자르지 않는다 — 목록 화면의 목적 자체가 "지금까지 남긴 기록을 전부 훑는 것"이라 상한을 두면 오래된 기록이 어디서도 보이지 않게
   * 된다. 한 사용자의 기록은 많아야 수백 건이라 한 번에 실어 보낸다(페이징이 필요해지면 {@code Pageable} 오버로드를 덧붙인다).
   *
   * <p>{@code JOIN FETCH}로 어종을 같이 끌고 온다 — 항목마다 어종명을 쓰므로, 없으면 지연 로딩이 기록 수만큼 추가 쿼리를 낸다(N+1).
   *
   * <p>정렬에 {@code id DESC}를 덧붙이는 이유는 다른 조회와 같다 — {@code createdAt}이 같은 순간의 기록끼리 순서가 매번 뒤바뀌지 않도록 id
   * 로 동점을 깬다. 다만 이 결과는 도감 외 기록과 <b>합쳐진 뒤 다시 정렬</b>되므로(서비스), 최종 순서를 여기서 단독으로 보장하지는 않는다.
   */
  @Query(
      "SELECT c FROM CatchRecord c JOIN FETCH c.fish "
          + "WHERE c.userId = :userId "
          + "ORDER BY c.createdAt DESC, c.id DESC")
  List<CatchRecord> findAllWithFishByUserId(@Param("userId") Long userId);

  /**
   * 소유자까지 함께 검증하는 단건 조회(기록 상세용). 어종을 함께 가져와 이름·서식지를 추가 쿼리 없이 쓴다.
   *
   * <p>{@code findById} 후 {@code userId}를 비교하는 방식을 쓰지 않는다 — "남의 기록"과 "없는 기록"이 코드상 다른 분기가 되어, 한쪽을
   * 빠뜨리면 남의 기록이 그대로 새어 나간다. 조건을 쿼리에 넣어 두 경우 모두 빈 결과 → 같은 404 로 수렴시킨다. → {@code
   * CustomFishRepository#findByIdAndUserId}
   */
  @Query("SELECT c FROM CatchRecord c JOIN FETCH c.fish WHERE c.id = :id AND c.userId = :userId")
  Optional<CatchRecord> findWithFishByIdAndUserId(
      @Param("id") Long id, @Param("userId") Long userId);

  /**
   * 특정 사용자가 특정 어종을 인증한 횟수.
   *
   * <p>인증 직후 "새로운 어종 획득 / N번째 인증"을 판정하려고 행 전체를 가져올 필요는 없어 COUNT 로만 센다(옵션 B에서 잡은 횟수는 저장된 값이 아니라 행
   * 개수에서 파생한다).
   */
  long countByUserIdAndFish_Id(Long userId, Long fishId);

  /**
   * 특정 사용자가 특정 어종을 잡은 <b>횟수와 최대 크기</b>를 한 번에 집계한다.
   *
   * <p>도감 상세 조회가 쓰는 경로다. 사진은 최근 4장만 내려주므로 응답 리스트로는 전체 횟수도 최대 크기도 계산할 수 없고, 둘 다 같은 조건의 기록 전체를 훑어야
   * 나오는 값이라 {@code COUNT}와 {@code MAX}를 한 쿼리로 묶었다.
   *
   * <p>기록이 없어도 행 하나가 돌아온다({@code catchCount} 0, {@code maxSize} {@code null}) — "안 잡은 어종"은 예외가 아니라
   * 정상 응답이기 때문이다. 인증 직후 횟수만 필요한 곳은 {@link #countByUserIdAndFish_Id(Long, Long)}를 그대로 쓴다.
   */
  @Query(
      "SELECT COUNT(c) AS catchCount, MAX(c.size) AS maxSize "
          + "FROM CatchRecord c "
          + "WHERE c.userId = :userId AND c.fish.id = :fishId")
  CatchStats findStatsByUserIdAndFishId(@Param("userId") Long userId, @Param("fishId") Long fishId);

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
