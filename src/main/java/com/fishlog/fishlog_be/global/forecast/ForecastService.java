package com.fishlog.fishlog_be.global.forecast;

import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import java.util.List;

/**
 * 스팟 예보 조회. 전체 예보를 Redis(12h TTL)에 캐시하고 스팟명으로 필터해 서빙한다. → docs/external.md §1, docs/spec.md
 *
 * <p>외부 호출 실패나 미매칭(담수 스팟 등) 시 예외를 던지지 않고 **빈 목록**을 반환한다(상세 응답 graceful degradation).
 */
public interface ForecastService {

  /**
   * 스팟명(seafsPstnNm)에 해당하는 예보 목록(예보일자 × 오전/오후).
   *
   * @return 매칭 예보 없음·외부 실패 시 빈 목록
   */
  List<SpotForecast> getForecast(String spotName);
}
