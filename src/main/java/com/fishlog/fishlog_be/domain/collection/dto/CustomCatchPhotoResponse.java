package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 도감 외 어종 사진 1장 = 등록 기록 1행. {@link CatchPhotoResponse}(도감 인증)와 <b>같은 역할·같은 구조</b>다 — 썸네일로 깔았다가 누르면
 * 오버레이에 크게 띄우면서 그때 입력한 크기·위치를 함께 보여주기 위한 단위.
 *
 * <p>필드명이 {@code catchRecordId}/{@code verifiedAt}이 아니라 {@code customCatchRecordId}/{@code
 * registeredAt}인 이유: id 가 가리키는 테이블이 다르고({@code custom_catch_record}), 이 기록은 <b>인증(verify)된 적이
 * 없다</b>. 등록 응답({@link CustomCatchResponse})과 같은 이름을 써서 한 기능 안에서 같은 값이 두 이름으로 불리지 않게 한다.
 */
@Schema(title = "CustomCatchPhotoResponse DTO", description = "도감 외 어종 사진 1장(사진 + 그때 기록한 크기·위치)")
public record CustomCatchPhotoResponse(
    @Schema(description = "기타 어종 기록 ID", example = "7") Long customCatchRecordId,
    @Schema(description = "사진 S3 URL", example = "https://.../custom-fish/uuid.jpg")
        String imageUrl,
    @Schema(description = "이 사진을 등록할 때 기록한 크기(cm)", example = "34.0") Double size,
    @Schema(description = "이 사진을 등록할 때 수기 입력한 위치(미입력 시 null)", example = "한탄강 고석정") String location,
    @Schema(description = "등록 시각(촬영 시각이 아니라 서버에 기록된 시각)", example = "2026-09-05T14:32:10")
        LocalDateTime registeredAt) {

  public static CustomCatchPhotoResponse from(CustomCatchRecord record) {
    return new CustomCatchPhotoResponse(
        record.getId(),
        record.getCertifiedImageUrl(),
        record.getSize(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }
}
