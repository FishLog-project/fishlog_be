package com.fishlog.fishlog_be.domain.collection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 내 도감 외 어종 전체 응답. {@link MyDexResponse}(내 도감)와 같은 자리의 DTO로, 목록 + 그 위에 띄울 수 두 개를 함께 담는다.
 *
 * <p>수 두 개의 의미가 도감과 다르다. 도감은 {@code totalCount}(전체 24종) 대비 {@code caughtCount}(잡은 종)로 <b>완성도</b>를
 * 나타내지만, 도감 외 어종에는 "전체 몇 종"이라는 분모가 없다(등록해야 생긴다). 그래서 여기서는 {@code totalCount}가 <b>내가 만든 어종 수</b>,
 * {@code totalCatchCount}가 <b>등록한 기록의 총 수</b>다 — "3종 · 총 7건"처럼 쓴다.
 */
@Schema(title = "MyCustomDexResponse DTO", description = "내 도감 외 어종 목록(어종 수 + 총 기록 수 + 어종 목록)")
public record MyCustomDexResponse(
    @Schema(description = "내가 등록한 도감 외 어종 수(= fishes 배열 길이)", example = "3") int totalCount,
    @Schema(description = "등록한 기록의 총 수(각 어종 catchCount의 합)", example = "7") int totalCatchCount,
    @Schema(description = "도감 외 어종 목록(최근에 잡은 어종부터)") List<CustomDexEntryResponse> fishes) {

  public static MyCustomDexResponse of(List<CustomDexEntryResponse> entries) {
    int totalCatchCount = entries.stream().mapToInt(CustomDexEntryResponse::catchCount).sum();
    return new MyCustomDexResponse(entries.size(), totalCatchCount, entries);
  }
}
