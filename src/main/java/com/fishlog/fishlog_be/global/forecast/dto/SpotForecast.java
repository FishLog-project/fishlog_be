package com.fishlog.fishlog_be.global.forecast.dto;

/**
 * 바다낚시지수 API 예보 1건(스팟 × 예보일자 × 오전/오후). 외부 응답에서 예보성 필드만 추린 값이며, Redis 캐시에 스팟명(seafsPstnNm) 기준으로 묶여
 * 저장된다. → docs/external.md §1
 *
 * <p>낚시지수·물때는 라벨/문자열일 수 있어 {@code String}, 날씨 수치(파고·수온·기온·유속·풍속)는 {@code Double}(파싱 실패 시 null)로 둔다.
 */
public record SpotForecast(
    String seafsPstnNm, // 낚시터 위치명(그룹 키)
    String predcYmd, // 예보 일자(YYYYMMDD)
    String predcNoonSeCd, // 오전/오후 구분
    String totalIndex, // 낚시지수(라벨)
    String lastScr, // 낚시지수 점수
    String tdlvHrScr, // 물때 점수
    String tdlvHrCn, // 물때 내용
    Double minWvhgt, // 최소 파고(m)
    Double maxWvhgt, // 최대 파고(m)
    Double minWtem, // 최소 수온(℃)
    Double maxWtem, // 최대 수온(℃)
    Double minArtmp, // 최소 기온(℃)
    Double maxArtmp, // 최대 기온(℃)
    Double minCrsp, // 최소 유속
    Double maxCrsp, // 최대 유속
    Double minWspd, // 최소 풍속
    Double maxWspd) {} // 최대 풍속
