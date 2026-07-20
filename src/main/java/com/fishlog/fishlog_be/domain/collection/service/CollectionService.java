package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;

/** 사용자 도감(어종 인증) 조회 서비스. */
public interface CollectionService {

  /**
   * 특정 사용자가 특정 어종을 인증한 기록 요약(잡은 횟수 + 사진 URL)을 조회한다.
   *
   * @param userId 조회 대상 사용자(임시 파라미터, 추후 로그인 사용자로 대체)
   * @param fishId 전체 도감 어종 id
   * @return 안 잡았어도 예외가 아니라 catchCount 0 · 빈 목록
   */
  CatchRecordResponse getMyCatch(Long userId, Long fishId);

  /**
   * 내 도감 그리드용 조회. 전체 수집 대상 어종을 순서대로 반환하되, 각 어종에 대해 {@code userId}가 잡았는지({@code caught})를 표시한다.
   *
   * <p>UI가 칸마다 이미지/그림자를 분기하도록 하기 위한 단일 조회다. 어종 목록은 전체 도감과 동일한 순서·집합이며, 잡은 어종 집합만 덧입힌다.
   *
   * @param userId 조회 대상 사용자(임시 파라미터, 추후 로그인 사용자로 대체)
   * @return 총 수·잡은 수·어종 목록(각 항목에 caught 포함)
   */
  MyDexResponse getMyDex(Long userId);
}
