package com.fishlog.fishlog_be.domain.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 랭킹 화면 응답(본인 순위 + Top3 + 전체). 완성도·크기 기준이 이 구조를 공유하며 {@code metric}으로 구분한다. → docs/ranking.md */
public record RankingResponse(
    @Schema(description = "랭킹 기준", example = "COMPLETION") RankingType metric,
    @Schema(description = "[완성도] 전체 도감 어종 수(완성도 분모). 크기 랭킹에서는 null", example = "29")
        Integer totalFishCount,
    @Schema(description = "본인 순위 블록. userId 미전달 시 null") RankingEntryResponse me,
    @Schema(description = "상위 3명") List<RankingEntryResponse> top3,
    @Schema(description = "전체 순위(내림차순)") List<RankingEntryResponse> rankings) {}
