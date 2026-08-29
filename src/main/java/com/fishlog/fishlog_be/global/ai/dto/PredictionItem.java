package com.fishlog.fishlog_be.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 모델 서버가 돌려주는 후보 1건. {@code species}는 {@code fishes.name}과 문자열이 정확히 일치해야 하는 <b>조인 키</b>다(모델 24종 ↔
 * 도감 24종 대조 완료). → docs/external.md §2
 *
 * <p>{@code confidence}는 25클래스 softmax 원값이라 후보 3개의 합이 1이 아니다. 보정(temperature scaling) 전 값이므로 과신 경향이
 * 있다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PredictionItem(int rank, String species, double confidence) {}
