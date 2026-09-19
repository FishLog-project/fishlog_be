package com.fishlog.fishlog_be.global.congestion;

import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import java.util.List;

/** 한국관광공사 관광지 집중률 예측 API 클라이언트. → docs/external.md §2 */
public interface CongestionClient {

  /**
   * 한 시군구의 집중률 예측을 전부 가져온다(관광지 수 × 30일).
   *
   * <p>관광지 하나씩 {@code tAtsNm}으로 조회할 수도 있지만 <b>시군구 단위로 한 번에 받는다</b>. 일 호출 한도가 1,000건이라 목록 30건에 30콜을
   * 쓰면 금방 소진되는 반면, 시군구 통째로는 1콜이면 끝나고 그 결과가 목록 전체를 채운다.
   *
   * @param areaCd 법정동 시도코드 2자리 (예: {@code 52})
   * @param signguCd 법정동 시군구코드 5자리 (예: {@code 52800})
   * @return 예측 목록. 해당 시군구에 데이터가 없으면 빈 리스트(전남 등)
   */
  List<CongestionRate> fetchBySigngu(String areaCd, String signguCd);
}
