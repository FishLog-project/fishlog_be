package com.fishlog.fishlog_be.domain.collection.repository;

/**
 * (사용자, 어종) 한 쌍에 대한 인증 기록 집계 프로젝션 — 잡은 횟수 + 최대 크기.
 *
 * <p>두 값을 <b>한 쿼리로 묶은 이유</b>: 둘 다 "그 어종의 기록 전체"를 훑어야 나오는 값인데, 도감 상세 조회는 사진을 최근 4장만 가져오므로 응답에 담긴
 * 리스트로는 어느 쪽도 계산할 수 없다. 따로 세면 같은 조건의 스캔이 두 번 도는 셈이라 `COUNT`와 `MAX`를 함께 받는다.
 *
 * <p>기록이 0건이어도 집계 쿼리는 <b>행 하나를 돌려준다</b> — {@code catchCount}는 0, {@code maxSize}는 {@code
 * null}이다(어종은 도감에 존재하는데 안 잡은 상태). 그래서 호출부는 결과가 {@code null}인지가 아니라 {@code maxSize}가 {@code null}인지를
 * 본다.
 */
public interface CatchStats {

  /** 잡은 총 횟수(인증 기록 행 개수). 사진 4장 제한과 무관한 전체 값. */
  long getCatchCount();

  /** 그 어종으로 기록한 크기(cm) 중 최댓값. 기록이 없으면 {@code null}. */
  Double getMaxSize();
}
