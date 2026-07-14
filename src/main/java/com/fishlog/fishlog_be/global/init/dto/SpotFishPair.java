package com.fishlog.fishlog_be.global.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * spot_fish_seed.json 의 (스팟, 어종) 페어 (fish_sopt 매핑 시드).
 *
 * <p>대상어종은 시점 불변이라 정적 매핑으로 저장한다. 플레이스홀더("-")는 수집 단계에서 제외됨. → docs/spec.md "스팟 데이터 설계"
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotFishPair(String spot, String fish) {}
