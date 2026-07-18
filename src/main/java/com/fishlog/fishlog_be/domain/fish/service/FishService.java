package com.fishlog.fishlog_be.domain.fish.service;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;

/** 전체 도감(마스터 어종 카탈로그) 조회 서비스. */
public interface FishService {

  /**
   * 전체 도감 목록(수집 대상 어종)과 총 수를 조회한다.
   *
   * @param name 어종명 완전일치 필터. {@code null}·공백이면 전체 목록, 값이 있으면 일치하는 어종만(0~1건) 반환한다. 못 찾아도 예외가 아니라 빈
   *     목록이다.
   */
  FishListResponse getFishList(String name);

  /**
   * 어종 상세를 조회한다.
   *
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 수집 대상 어종이 없으면 {@code
   *     FISH_NOT_FOUND}
   */
  FishDetailResponse getFishDetail(Long id);
}
