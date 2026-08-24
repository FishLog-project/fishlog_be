package com.fishlog.fishlog_be.global.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * inland_detail_seed.json 전체 구조 (inland_spot_detail 테이블 시드).
 *
 * <p>data/spot/build_seed.py 산출물. {@code source}/{@code unit} 등 메타 필드는 참고용이고 {@code details} 배열만
 * 적재에 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InlandDetailSeedData(
    String source, String unit, int spotCount, List<InlandDetailSeed> details) {}
