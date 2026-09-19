package com.fishlog.fishlog_be.global.congestion.dto;

/**
 * 관광지 집중률 예측 1건(관광지 × 날짜). 집중률 API는 조회일 기준 <b>향후 30일치</b>를 한 관광지당 30행으로 돌려주므로, 한 시군구를 조회하면 (관광지 수 ×
 * 30)행이 온다. → docs/external.md §2
 *
 * @param baseYmd 예측 기준일 {@code yyyyMMdd}
 * @param tAtsNm 관광지명 (집중률 데이터셋 표기 — 예: {@code 채석강 (전북 서해안 국가지질공원)})
 * @param rate 집중률 지수 0~100 (2018년 이후 피크를 100으로 정규화한 값, 방문자 수나 %가 아니다)
 */
public record CongestionRate(String baseYmd, String tAtsNm, double rate) {}
