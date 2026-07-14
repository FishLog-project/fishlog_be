package com.fishlog.fishlog_be.global.init.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * spots_seed.json 전체 구조 (spots 테이블 시드).
 *
 * <p>data/spot/seed.py 산출물. {@code source}/{@code totalRecords} 등 메타 필드는 무시하고 {@code spots} 배열만
 * 사용한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotSeedData(String source, int spotCount, List<SpotSeed> spots) {}
