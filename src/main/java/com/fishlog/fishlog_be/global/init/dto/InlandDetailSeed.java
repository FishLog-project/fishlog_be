package com.fishlog.fishlog_be.global.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * inland_detail_seed.json 의 담수 스팟 1곳 상세(하폭·유수폭·수심, 단위 m).
 *
 * <p>{@code spot}은 {@code spots.name}(UNIQUE)과 매칭되는 키다. 조사에서 빠진 항목은 개별 null 로 온다. →
 * data/spot/build_seed.py
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InlandDetailSeed(
    String spot,
    Double riverWidthMin,
    Double riverWidthMax,
    Double flowWidthMin,
    Double flowWidthMax,
    Double depthMin,
    Double depthMax) {}
