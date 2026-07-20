package com.fishlog.fishlog_be.domain.spot.service;

import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import java.util.List;

/** 낚시 스팟 조회 서비스. → docs/spec.md */
public interface SpotService {

  /** 스팟 전체 목록 조회(지도 마커용). */
  List<SpotResponse> getSpots();
}
