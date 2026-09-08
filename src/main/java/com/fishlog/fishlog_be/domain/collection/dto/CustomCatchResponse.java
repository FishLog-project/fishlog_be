package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 도감 외 어종 등록 완료 응답.
 *
 * <p>{@code customFishId}는 <b>어종</b>의 id 다({@code customCatchRecordId}는 이번 기록 1건의 id). 같은 이름으로 다시
 * 등록하면 기록 id 는 매번 새로 생기지만 어종 id 는 그대로이며, 상세 조회({@code GET /api/collections/custom?customFishId=})는
 * 이 값을 쓴다.
 *
 * <p>{@link VerifyResponse}에 있는 {@code firstCatch}·{@code catchCount}가 여기에는 없다. 그 값들은 "같은 어종을 몇 번째로
 * 잡았나"를 (user, fish) 행 집계로 파생한 것인데, 여기서 어종은 <b>검증되지 않은 자유 텍스트</b>라 같은 물고기를 "우럭"·"조피볼락"처럼 다르게 적으면 다른
 * 어종으로 세어진다. 신뢰할 수 없는 숫자를 "N번째 인증"으로 연출하느니 내려주지 않는다. → docs/spec.md
 */
@Schema(title = "CustomCatchResponse DTO", description = "도감 외 어종 등록 결과")
public record CustomCatchResponse(
    @Schema(description = "생성된 기타 어종 기록 ID", example = "7") Long customCatchRecordId,
    @Schema(description = "이 기록이 속한 도감 외 어종 ID(상세 조회 시 customFishId 로 사용)", example = "3")
        Long customFishId,
    @Schema(description = "사용자가 입력한 어종명", example = "쏘가리") String fishName,
    @Schema(description = "사용자가 입력한 주요 서식지(미입력 시 null)", example = "강") String habitat,
    @Schema(description = "업로드된 사진 S3 URL", example = "https://.../custom-fish/uuid.jpg")
        String imageUrl,
    @Schema(description = "기록한 크기(cm)", example = "34.0") Double size,
    @Schema(description = "기록한 잡은 위치(수기 입력, 미입력 시 null)", example = "한탄강 고석정") String location,
    @Schema(description = "등록 시각(촬영 시각이 아니라 서버에 기록된 시각)", example = "2026-09-05T14:32:10")
        LocalDateTime registeredAt) {

  public static CustomCatchResponse from(CustomCatchRecord record) {
    CustomFish fish = record.getCustomFish();
    return new CustomCatchResponse(
        record.getId(),
        fish.getId(),
        fish.getName(),
        fish.getHabitat(),
        record.getCertifiedImageUrl(),
        record.getSize(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }
}
