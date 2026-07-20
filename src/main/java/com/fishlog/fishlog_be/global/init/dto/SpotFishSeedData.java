package com.fishlog.fishlog_be.global.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * spot_fish_seed.json 전체 구조 (major_fish 매핑 시드).
 *
 * <p>data/spot/seed.py 산출물. {@code fishes}는 고유 어종 목록, {@code pairs}는 (스팟, 어종) 페어.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotFishSeedData(
    String source,
    int spotCount,
    int fishCount,
    int pairCount,
    List<String> fishes,
    List<SpotFishPair> pairs) {}
