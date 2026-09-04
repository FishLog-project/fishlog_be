package com.fishlog.fishlog_be.domain.spot.service;

/** 스팟 조회수 집계. 상세 조회 시 비동기로 호출한다. → docs/spec.md */
public interface SpotViewService {

  /**
   * 스팟 조회를 1회 기록한다(비동기). 사용자/IP별 **1일 1회**만 증가한다(중복 집계 방지).
   *
   * @param spotId 조회한 스팟 id
   * @param userId 로그인 사용자 id(비로그인이면 null)
   * @param clientIp 비로그인 dedup용 클라이언트 IP
   */
  void recordView(Long spotId, Long userId, String clientIp);
}
