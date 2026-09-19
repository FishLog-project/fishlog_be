package com.fishlog.fishlog_be.global.congestion;

import java.util.Map;

/** 관광지 당일 집중률(혼잡도) 조회. → docs/external.md §2 */
public interface CongestionService {

  /**
   * 한 시군구의 <b>오늘자</b> 집중률을 관광지명→지수 맵으로 돌려준다. 시군구 단위 응답을 당일 만료로 캐시하므로, 같은 지역 요청이 반복돼도 외부 호출은 하루 한
   * 번이다.
   *
   * <p><b>실패해도 예외를 던지지 않는다.</b> 혼잡도는 관광 목록에 곁들이는 부가 정보라, 외부 장애·쿼터 소진 때문에 목록 조회 자체가 실패하면 안 된다. 실패는 빈
   * 맵으로 떨어지고 호출부는 혼잡도 없이 응답한다.
   *
   * @param areaCd 법정동 시도코드 2자리 (예: {@code 52})
   * @param signguCd 법정동 시군구코드 5자리 (예: {@code 52800})
   * @return 관광지명 → 집중률(0~100). 데이터 없는 지역(전남 등)·조회 실패는 빈 맵
   */
  Map<String, Double> getTodayRates(String areaCd, String signguCd);
}
