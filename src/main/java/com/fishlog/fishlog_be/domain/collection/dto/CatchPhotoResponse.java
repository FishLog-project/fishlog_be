package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 인증 사진 1장 = 인증 기록 1행(옵션 B). 도감 상세에서 썸네일로 깔았다가, 누르면 오버레이에 크게 띄우면서 그때 입력한 크기·위치를 함께 보여주기 위한 단위다.
 *
 * <p>URL 배열이 아니라 <b>객체 배열</b>인 이유: 크기·위치는 사진마다 다른 값이라 URL만 내려주면 프론트가 인덱스를 맞춰 다른 배열과 짝지어야 한다. 한 행에
 * 담아 보내면 오버레이가 객체 하나만 들고 그릴 수 있다.
 */
@Schema(title = "CatchPhotoResponse DTO", description = "인증 사진 1장(사진 + 그때 기록한 크기·위치)")
public record CatchPhotoResponse(
    @Schema(description = "인증 기록 ID", example = "42") Long catchRecordId,
    @Schema(description = "인증 사진 S3 URL", example = "https://.../fish/uuid.jpg") String imageUrl,
    @Schema(description = "이 사진을 인증할 때 기록한 크기(cm)", example = "27.5") Double size,
    @Schema(description = "이 사진을 인증할 때 수기 입력한 위치(미입력 시 null)", example = "충주호 종댕이길 선착장")
        String location,
    @Schema(description = "인증을 등록한 시각(촬영 시각이 아니라 서버에 기록된 시각)", example = "2026-09-01T14:32:10")
        LocalDateTime verifiedAt) {

  public static CatchPhotoResponse from(CatchRecord record) {
    return new CatchPhotoResponse(
        record.getId(),
        record.getCertifiedImageUrl(),
        record.getSize(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }
}
