package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 특정 어종에 대한 내 인증 요약(잡은 횟수 + 인증 사진 목록). */
public record CatchRecordResponse(
    @Schema(description = "잡은 횟수(인증 기록 수)", example = "3") int catchCount,
    @Schema(description = "인증 사진 S3 URL 목록") List<String> imageUrls) {

  /**
   * 인증 기록 목록으로부터 응답을 만든다. 안 잡은 어종이면 빈 리스트가 들어와 catchCount 0 · imageUrls []가 된다.
   *
   * <p>옵션 B의 핵심: 잡은 횟수는 어딘가 저장된 값이 아니라 {@code records}의 "개수"에서 파생한다.
   */
  public static CatchRecordResponse of(List<CatchRecord> records) {
    List<String> imageUrls = records.stream().map(CatchRecord::getCertifiedImageUrl).toList();
    return new CatchRecordResponse(records.size(), imageUrls);
  }
}
