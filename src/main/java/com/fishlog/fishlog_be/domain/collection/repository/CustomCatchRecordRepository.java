package com.fishlog.fishlog_be.domain.collection.repository;

import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomCatchRecordRepository extends JpaRepository<CustomCatchRecord, Long> {

  /**
   * 특정 사용자가 등록한 도감 외 어종 기록 <b>전체</b>를 최신순으로, 어종을 함께 조회한다.
   *
   * <p>목록 화면이 어종마다 이름·서식지·최근 사진·횟수·최대 크기를 모두 쓰므로 {@code JOIN FETCH}로 어종을 같이 끌고 온다 — 없으면 지연 로딩이 어종
   * 수만큼 추가 쿼리를 낸다(N+1).
   *
   * <p>어종별 집계(횟수·최대 크기)를 DB {@code GROUP BY}로 하지 않는 이유: 그러면 각 어종의 최근 사진을 다시 조회해야 해서 결국 어종 수만큼 쿼리가
   * 늘어난다. 한 사용자의 기록은 많아야 수십~수백 건이라 <b>한 번에 받아 메모리에서 묶는 편이 쿼리 1회</b>로 끝난다.
   *
   * <p>정렬에 {@code id DESC}를 덧붙인 이유는 {@code catch_record} 조회와 같다 — {@code createdAt}이 같은 순간의 기록끼리
   * 순서가 매번 뒤바뀌지 않도록 id 로 동점을 깬다. 이 정렬은 그룹 안에서 "최근 N장"을 앞에서 잘라 쓰는 근거이므로 호출부가 재정렬하지 않는다.
   */
  @Query(
      "SELECT c FROM CustomCatchRecord c JOIN FETCH c.customFish "
          + "WHERE c.userId = :userId "
          + "ORDER BY c.createdAt DESC, c.id DESC")
  List<CustomCatchRecord> findAllWithFishByUserId(@Param("userId") Long userId);

  /**
   * 특정 어종의 기록을 최신순으로 조회한다(상세 화면의 최근 사진용).
   *
   * <p>{@code Pageable}로 개수를 제한해 기록이 수백 건이어도 응답 크기가 고정된다. 어종 소유자 검증은 호출부가 어종을 먼저 조회하며 끝내므로 여기서는
   * {@code customFishId}만 본다.
   */
  List<CustomCatchRecord> findByCustomFish_IdOrderByCreatedAtDescIdDesc(
      Long customFishId, Pageable pageable);

  /**
   * 특정 어종의 <b>등록 횟수와 최대 크기</b>를 한 번에 집계한다. 도감 상세({@code
   * CatchRecordRepository#findStatsByUserIdAndFishId})와 같은 이유·같은 프로젝션을 쓴다 — 사진을 4장으로 자르므로 응답 리스트로는
   * 둘 다 계산할 수 없다.
   */
  @Query(
      "SELECT COUNT(c) AS catchCount, MAX(c.size) AS maxSize "
          + "FROM CustomCatchRecord c "
          + "WHERE c.customFish.id = :customFishId")
  CatchStats findStatsByCustomFishId(@Param("customFishId") Long customFishId);

  /**
   * 회원탈퇴 시 해당 사용자의 도감 외 어종 기록을 모두 삭제한다(하드).
   *
   * <p>{@code catch_record}와 마찬가지로 {@code user_id}가 FK 가 아니라 DB 캐스케이드가 없다. 남겨 두면 어느 화면에서도 조회되지 않는 채
   * S3 사진만 붙들고 있는 고아 데이터가 된다. → {@code CatchRecordRepository#deleteByUserId}
   */
  void deleteByUserId(Long userId);
}
