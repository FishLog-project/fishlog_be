package com.fishlog.fishlog_be.domain.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 랭킹 한 줄(사용자 1명). 완성도/크기 두 기준이 공유하는 항목으로, 해당 기준에 쓰이지 않는 점수 필드는 {@code null}이다.
 *
 * <ul>
 *   <li>완성도 기준: {@code caughtCount}, {@code completionRate} 사용 (maxSize=null)
 *   <li>크기 기준: {@code maxSize} 사용 (caughtCount·completionRate=null)
 * </ul>
 *
 * <p>{@code nickname}은 {@code users}에서 조회해 채운다(탈퇴 등으로 조회 실패 시 {@code null}). → docs/ranking.md,
 * docs/auth-followup.md
 */
public record RankingEntryResponse(
    @Schema(description = "순위(공동 순위 1,1,3). 본인이 기록이 없으면 null", example = "1") Integer rank,
    @Schema(description = "사용자 id", example = "7") Long userId,
    @Schema(description = "닉네임(users 조회). 사용자를 찾지 못하면 null", example = "낚시왕", nullable = true)
        String nickname,
    @Schema(description = "[완성도] 인증한 고유 어종 수", example = "27") Integer caughtCount,
    @Schema(description = "[완성도] 완성도 퍼센트(소수 1자리)", example = "93.1") Double completionRate,
    @Schema(description = "[크기] 최대 어종 크기(cm)", example = "88.0") Double maxSize) {

  /** 완성도 랭킹 한 줄. */
  public static RankingEntryResponse completion(
      Integer rank, Long userId, String nickname, Integer caughtCount, Double completionRate) {
    return new RankingEntryResponse(rank, userId, nickname, caughtCount, completionRate, null);
  }

  /** 크기 랭킹 한 줄. */
  public static RankingEntryResponse size(
      Integer rank, Long userId, String nickname, Double maxSize) {
    return new RankingEntryResponse(rank, userId, nickname, null, null, maxSize);
  }
}
