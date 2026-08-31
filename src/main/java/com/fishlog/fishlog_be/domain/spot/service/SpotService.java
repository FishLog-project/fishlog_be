package com.fishlog.fishlog_be.domain.spot.service;

import com.fishlog.fishlog_be.domain.spot.dto.SpotDetailResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import java.util.List;

/** 낚시 스팟 조회 서비스. → docs/spec.md */
public interface SpotService {

  /** 스팟 전체 목록 조회(지도 마커용). 각 항목에 로그인 사용자의 찜 여부(isFavorite)를 병합한다. */
  List<SpotResponse> getSpots(Long userId);

  /**
   * 스팟 상세 조회. DB 기본정보 + 대상 어종에 실시간 예보를 병합한다.
   *
   * @param id 스팟 id
   * @return 상세 응답(예보 실패/미매칭 시 forecast=null)
   */
  SpotDetailResponse getSpotDetail(Long id);
}
