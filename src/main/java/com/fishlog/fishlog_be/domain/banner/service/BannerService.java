package com.fishlog.fishlog_be.domain.banner.service;

import com.fishlog.fishlog_be.domain.fish.dto.SeasonalFishResponse;
import java.util.List;

/** 홈 배너 콘텐츠 조회 서비스. */
public interface BannerService {

  /**
   * 현재(서버 시각, KST) 월이 속한 계절에 제철인 어종 중 최대 {@code count}종을 랜덤으로 뽑아 반환한다. 제철 어종이 {@code count}보다 적으면
   * 있는 만큼만 반환한다(예외 아님).
   *
   * @param count 뽑을 최대 어종 수(양수)
   */
  List<SeasonalFishResponse> getSeasonalFishRecommendations(int count);
}
