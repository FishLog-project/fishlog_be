package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecordType;
import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 인증 기록 <b>1건</b>의 상세. 목록({@link CatchHistoryEntryResponse})의 한 줄을 눌러 들어오는 화면이며, 도감 인증과 도감 외 등록이 같은
 * 모양으로 온다.
 *
 * <p>목록 항목에 {@code habitat} 하나가 더 있는 형태다. 목록에서는 줄마다 서식지를 그리지 않아 전체 기록 수만큼 실어 보낼 이유가 없지만, 상세는 사진 한
 * 장을 크게 띄우며 어종 정보를 함께 보여주는 자리라 필요하다.
 *
 * <p><b>서식지는 기록이 아니라 어종의 속성</b>이라 어종에서 읽는다 — {@code DEX}면 도감({@code fishes.habitat}, 시드가 채운 값),
 * {@code CUSTOM}이면 사용자가 등록할 때 적은 값({@link CustomFish#getHabitat()})이다. 둘 다 비어 있을 수 있어 {@code null}을
 * 허용한다.
 */
@Schema(title = "CatchRecordDetailResponse DTO", description = "인증 기록 1건 상세")
public record CatchRecordDetailResponse(
    @Schema(description = "기록 ID", example = "42") Long recordId,
    @Schema(description = "기록 종류 — DEX(도감 인증) / CUSTOM(도감 외 수기 등록)", example = "DEX")
        CatchRecordType recordType,
    @Schema(
            description = "어종 ID. DEX 면 도감 어종 id, CUSTOM 이면 도감 외 어종 id(customFishId)",
            example = "1")
        Long fishId,
    @Schema(description = "어종명", example = "감성돔") String fishName,
    @Schema(description = "어종 서식지(없으면 null)", example = "바다") String habitat,
    @Schema(description = "기록한 크기(cm)", example = "31.0") Double size,
    @Schema(description = "사진 S3 URL", example = "https://.../fish/uuid.jpg") String imageUrl,
    @Schema(description = "기록한 잡은 위치(수기 입력, 미입력 시 null)", example = "충주호 종댕이길 선착장") String location,
    @Schema(description = "기록이 서버에 등록된 시각(촬영 시각이 아님)", example = "2026-09-01T14:32:10")
        LocalDateTime recordedAt) {

  /** 도감 인증 기록 상세. 서식지는 도감 어종의 값이다. */
  public static CatchRecordDetailResponse from(CatchRecord record) {
    return new CatchRecordDetailResponse(
        record.getId(),
        CatchRecordType.DEX,
        record.getFish().getId(),
        record.getFish().getName(),
        record.getFish().getHabitat(),
        record.getSize(),
        record.getCertifiedImageUrl(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }

  /** 도감 외 어종 기록 상세. 서식지는 사용자가 등록할 때 적은 값이라 {@code null}이 흔하다. */
  public static CatchRecordDetailResponse from(CustomCatchRecord record) {
    CustomFish fish = record.getCustomFish();
    return new CatchRecordDetailResponse(
        record.getId(),
        CatchRecordType.CUSTOM,
        fish.getId(),
        fish.getName(),
        fish.getHabitat(),
        record.getSize(),
        record.getCertifiedImageUrl(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }
}
