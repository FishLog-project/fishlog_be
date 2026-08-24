package com.fishlog.fishlog_be.global.forecast;

import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import java.util.List;

/**
 * 바다낚시지수 API(공공데이터포털 15142486) 호출 어댑터. → docs/external.md §1
 *
 * <p>예보성 데이터는 저장하지 않으므로, 상세 조회 시점에 이 클라이언트로 전체 예보를 받아 캐시한다.
 */
public interface FishingIndexClient {

  /**
   * 전체 예보를 수집한다(갯바위·선상 두 구분, 페이지네이션 전량).
   *
   * @return 예보 1건 목록(스팟 × 예보일자 × 오전/오후). 호출 실패 시 예외를 던진다(호출부에서 폴백).
   */
  List<SpotForecast> fetchAll();
}
