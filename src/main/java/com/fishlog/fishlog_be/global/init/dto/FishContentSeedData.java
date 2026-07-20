package com.fishlog.fishlog_be.global.init.dto;

import java.util.List;

/**
 * fish_content_seed.json 루트 — 어종 도감 콘텐츠 목록.
 *
 * @param fishes 어종별 콘텐츠 시드
 */
public record FishContentSeedData(List<FishContentSeed> fishes) {}
