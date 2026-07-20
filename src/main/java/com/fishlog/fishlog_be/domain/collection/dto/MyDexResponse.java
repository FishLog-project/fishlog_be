package com.fishlog.fishlog_be.domain.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 내 도감 전체 응답. 전체 수집 대상 어종 목록(각 칸의 {@code caught} 포함)과, 완성도 표시에 쓰는 총 수·잡은 수를 함께 담는다.
 *
 * <p>{@code caughtCount}/{@code totalCount}는 도감 완성도(랭킹 분자/분모)와 같은 값이라 별도 조회 없이 이 응답만으로 진행도를 그릴 수
 * 있다. → docs/ranking.md
 */
@Schema(title = "MyDexResponse DTO", description = "내 도감 목록(총 수 + 잡은 수 + 어종 목록)")
public record MyDexResponse(
    @Schema(description = "전체 수집 대상 어종 수", example = "24") int totalCount,
    @Schema(description = "그 중 내가 잡은 어종 수(완성도 분자)", example = "12") int caughtCount,
    @Schema(description = "도감 항목 목록") List<DexEntryResponse> fishes) {

  public static MyDexResponse of(List<DexEntryResponse> entries) {
    int caughtCount = (int) entries.stream().filter(DexEntryResponse::caught).count();
    return new MyDexResponse(entries.size(), caughtCount, entries);
  }
}
