package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecordType;
import com.fishlog.fishlog_be.domain.collection.entity.CustomCatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * 인증 기록 목록의 한 줄 — "어종명 (크기)"로 읽히는 <b>기록 1건</b>. 도감 인증({@link CatchRecord})과 도감 외 등록({@link
 * CustomCatchRecord})이 <b>같은 모양</b>으로 들어온다.
 *
 * <p>기존 조회 DTO들이 모두 <b>어종 단위</b>({@link DexEntryResponse}·{@link CustomDexEntryResponse})인 것과 달리
 * 이쪽은 <b>기록 단위</b>다. 같은 어종을 세 번 잡았으면 도감 그리드에는 칸이 하나지만 이 목록에는 세 줄이 뜬다.
 *
 * <p><b>{@code recordId}만으로 기록을 특정할 수 없다</b> — 두 테이블 모두 id 가 1부터 증가해 값이 겹치기 때문이다. 단건 조회({@code GET
 * /api/collections/records/&#123;recordId&#125;?type=})에는 {@code recordType}을 함께 보내야 하며, 이 응답의 두
 * 필드를 그대로 옮기면 된다. → {@link CatchRecordType}
 *
 * <p>{@code fishId}는 <b>{@code recordType}에 따라 가리키는 대상이 다르다</b>: {@code DEX}면 도감 어종 id({@code GET
 * /api/fish/&#123;id&#125;}·{@code GET /api/collections?fishId=}), {@code CUSTOM}이면 사용자별 어종
 * id({@code GET /api/collections/custom?customFishId=})다. 한 줄에서 "이 어종의 다른 기록도 보기"로 넘어갈 때 쓴다.
 *
 * <p>시각 필드 이름이 {@code verifiedAt}({@link CatchPhotoResponse})도 {@code registeredAt}({@link
 * CustomCatchPhotoResponse})도 아닌 {@code recordedAt}인 이유: 이 목록에는 인증된 기록과 인증되지 않은 수기 기록이 <b>섞여
 * 들어온다</b>. 어느 한쪽 이름을 쓰면 나머지 절반에는 사실이 아닌 이름이 붙는다.
 */
@Schema(title = "CatchHistoryEntryResponse DTO", description = "내 인증 기록 1건(목록 한 줄)")
public record CatchHistoryEntryResponse(
    @Schema(description = "기록 ID. 단건 조회 시 recordType 과 함께 사용", example = "42") Long recordId,
    @Schema(description = "기록 종류 — DEX(도감 인증) / CUSTOM(도감 외 수기 등록)", example = "DEX")
        CatchRecordType recordType,
    @Schema(
            description = "어종 ID. DEX 면 도감 어종 id, CUSTOM 이면 도감 외 어종 id(customFishId)",
            example = "1")
        Long fishId,
    @Schema(description = "어종명", example = "감성돔") String fishName,
    @Schema(description = "기록한 크기(cm)", example = "31.0") Double size,
    @Schema(description = "사진 S3 URL", example = "https://.../fish/uuid.jpg") String imageUrl,
    @Schema(description = "기록한 잡은 위치(수기 입력, 미입력 시 null)", example = "충주호 종댕이길 선착장") String location,
    @Schema(description = "기록이 서버에 등록된 시각(촬영 시각이 아님)", example = "2026-09-01T14:32:10")
        LocalDateTime recordedAt) {

  /**
   * 목록 정렬 기준 — <b>최신순</b>.
   *
   * <p>두 테이블에서 각각 최신순으로 받아 온 목록을 합치면 그 순간 순서가 깨지므로, 합친 뒤 이 비교자로 다시 정렬한다.
   *
   * <p>{@code recordedAt} 동점을 {@code recordType} → {@code recordId} 내림차순으로 깬다. <b>서로 다른 테이블의 id 는
   * 크기 비교에 의미가 없어</b>(한쪽 42가 다른 쪽 42보다 나중이라는 보장이 없다) 먼저 종류로 묶고, 같은 종류 안에서만 id 로 비교한다. 무엇이 위로 오든
   * 상관없지만 <b>매번 같은 순서여야</b> 새로고침할 때 목록이 흔들리지 않는다.
   */
  public static final Comparator<CatchHistoryEntryResponse> LATEST_FIRST =
      Comparator.comparing(CatchHistoryEntryResponse::recordedAt, Comparator.reverseOrder())
          .thenComparing(CatchHistoryEntryResponse::recordType)
          .thenComparing(CatchHistoryEntryResponse::recordId, Comparator.reverseOrder());

  /** 도감 인증 기록 1건 → 목록 한 줄. 어종은 {@code JOIN FETCH}로 함께 조회된 것을 쓴다. */
  public static CatchHistoryEntryResponse from(CatchRecord record) {
    return new CatchHistoryEntryResponse(
        record.getId(),
        CatchRecordType.DEX,
        record.getFish().getId(),
        record.getFish().getName(),
        record.getSize(),
        record.getCertifiedImageUrl(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }

  /** 도감 외 어종 기록 1건 → 목록 한 줄. 어종명은 기록이 아니라 어종({@link CustomFish})에 있다. */
  public static CatchHistoryEntryResponse from(CustomCatchRecord record) {
    CustomFish fish = record.getCustomFish();
    return new CatchHistoryEntryResponse(
        record.getId(),
        CatchRecordType.CUSTOM,
        fish.getId(),
        fish.getName(),
        record.getSize(),
        record.getCertifiedImageUrl(),
        record.getCatchLocation(),
        record.getCreatedAt());
  }
}
