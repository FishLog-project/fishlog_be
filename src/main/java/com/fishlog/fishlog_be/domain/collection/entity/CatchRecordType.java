package com.fishlog.fishlog_be.domain.collection.entity;

/**
 * 인증 기록이 어느 테이블에서 온 것인지 구분하는 값. 컬럼이 아니라 <b>조회 응답·조회 파라미터 전용</b>이라 {@code @Entity}가 아니다.
 *
 * <p><b>왜 필요한가:</b> 사용자에게는 "내가 남긴 기록"이 하나의 목록이지만 저장은 {@link CatchRecord}(도감 24종)와 {@link
 * CustomCatchRecord}(도감 외 수기 어종) 두 테이블로 나뉜다. 두 테이블 모두 id 가 1부터 증가하므로 합친 목록에서는 <b>{@code recordId}
 * 하나만으로 기록을 특정할 수 없다</b>(id 42 가 양쪽에 다 있을 수 있다). 그래서 단건 조회({@code GET
 * /api/collections/records/{recordId}?type=})는 이 값을 함께 받아 <b>(id, type) 쌍</b>을 복합 식별자로 쓴다.
 *
 * <p>id 를 {@code "DEX-42"} 같은 문자열로 합치는 방법도 있지만, 그러면 파싱이 필요하고 프론트가 id 를 숫자로 다루지 못한다. 두 필드로 나눠 두면 응답을
 * 그대로 다음 요청의 파라미터로 옮기면 된다.
 */
public enum CatchRecordType {
  /** 도감 24종 인증 기록({@code catch_record}). 도감 그리드·랭킹에 반영되는 기록이다. */
  DEX,

  /** 도감 외 어종 수기 등록 기록({@code custom_catch_record}). 도감·랭킹 집계에서 제외되는 기록이다. */
  CUSTOM
}
