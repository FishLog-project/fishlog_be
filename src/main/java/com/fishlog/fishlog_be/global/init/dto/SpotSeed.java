package com.fishlog.fishlog_be.global.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * spots_seed.json 의 개별 스팟(불변 정보).
 *
 * <p>바다낚시지수 API(15142486)에서 수집한 위치명·위도·경도. → docs/spec.md "spots 테이블", data/spot/seed.py
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotSeed(String name, double lat, double lot) {}
