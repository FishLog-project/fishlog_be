package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;

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
}
