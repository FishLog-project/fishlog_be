package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 특정 어종에 대한 내 인증 요약(서식지 + 잡은 횟수 + 최근 인증 사진).
 *
 * <p>{@code habitat}은 인증 기록이 아니라 <b>어종</b>의 속성이라 기록이 0건이어도 채워진다 — 그래서 기록 목록이 아닌 별도 인자로 받는다. 안 잡은
 * 어종은 기록이 비어 있어 거기서 어종을 끌어올 수 없기 때문이다.
 *
 * <p><b>{@code catchCount}·{@code maxSize}는 자르지 않은 전체 기록 기준</b>이고 {@code recentCatches}만 최대 4장으로
 * 제한된다. 최대 크기가 5번째로 오래된 기록에 있어도 {@code maxSize}에는 잡히므로, 응답에 온 사진 4장에서 계산한 값과 다를 수 있다(그게 맞다). 둘을 같은
 * 값으로 맞추면 "더 있다"는 사실이 사라지므로, 프론트는 {@code catchCount - recentCatches.size()}로 "+N장 더" 배지를 그릴 수 있다.
 */
@Schema(title = "CatchRecordResponse DTO", description = "특정 어종에 대한 내 인증 요약")
public record CatchRecordResponse(
    @Schema(description = "어종 서식지(바다/강/저수지/하천)", example = "바다") String habitat,
    @Schema(description = "잡은 총 횟수(인증 기록 수). 사진 4장 제한과 무관한 전체 값", example = "7") int catchCount,
    @Schema(
            description = "이 어종으로 잡은 것 중 가장 큰 크기(cm). 한 번도 안 잡았으면 null",
            example = "31.0",
            nullable = true)
        Double maxSize,
    @Schema(description = "최근 인증 사진(최신순, 최대 4장). 각 항목에 그때의 크기·위치 포함")
        List<CatchPhotoResponse> recentCatches) {

  /**
   * 최근 인증 기록으로부터 응답을 만든다. 안 잡은 어종이면 빈 리스트가 들어와 {@code recentCatches:[]}가 된다(서식지는 그대로 채워진다).
   *
   * <p>옵션 B의 핵심: 잡은 횟수는 어딘가 저장된 값이 아니라 행 개수에서 파생한다. 다만 여기서는 {@code recentRecords}가 이미 4장으로 잘려 있으므로
   * 리스트 크기가 아니라 <b>따로 센 전체 개수</b>를 받는다.
   *
   * @param habitat 어종의 서식지. 콘텐츠 시드가 채우지 않은 어종은 {@code null}일 수 있다
   * @param catchCount 자르기 전 전체 인증 횟수
   * @param maxSize 자르기 전 전체 기록의 최대 크기(cm). 기록이 0건이면 {@code null}
   * @param recentRecords 최신순으로 잘라온 인증 기록(최대 4건)
   */
  public static CatchRecordResponse of(
      String habitat, int catchCount, Double maxSize, List<CatchRecord> recentRecords) {
    List<CatchPhotoResponse> photos = recentRecords.stream().map(CatchPhotoResponse::from).toList();
    return new CatchRecordResponse(habitat, catchCount, maxSize, photos);
  }
}
