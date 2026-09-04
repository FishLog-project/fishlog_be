package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 어종 인증(도감 기록) 완료 응답.
 *
 * <p>{@code firstCatch}·{@code catchCount}는 저장된 컬럼이 아니라 (user, fish) 행 집계에서 파생한 값이다(옵션 B). 도감 화면이
 * "새로운 어종 획득!" / "N번째 인증" 연출을 바로 띄울 수 있도록 함께 내려준다. → docs/spec.md
 */
@Schema(title = "VerifyResponse DTO", description = "어종 인증 완료 결과")
public record VerifyResponse(
    @Schema(description = "생성된 인증 기록 ID", example = "42") Long catchRecordId,
    @Schema(description = "인증한 어종 ID", example = "15") Long fishId,
    @Schema(description = "인증한 어종명", example = "붕어") String fishName,
    @Schema(description = "업로드된 인증 사진 S3 URL", example = "https://.../fish/uuid.jpg")
        String imageUrl,
    @Schema(description = "기록한 크기(cm)", example = "27.5") Double size,
    @Schema(description = "기록한 잡은 위치(수기 입력, 미입력 시 null)", example = "충주호 종댕이길 선착장") String location,
    @Schema(description = "이 어종을 처음 잡았는지(도감 새 칸 획득 여부)", example = "true") boolean firstCatch,
    @Schema(description = "이 어종을 지금까지 잡은 총 횟수(이번 인증 포함)", example = "1") int catchCount) {

  public static VerifyResponse of(CatchRecord record, String fishName, int catchCount) {
    return new VerifyResponse(
        record.getId(),
        record.getFish().getId(),
        fishName,
        record.getCertifiedImageUrl(),
        record.getSize(),
        record.getCatchLocation(),
        catchCount == 1,
        catchCount);
  }
}
