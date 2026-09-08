package com.fishlog.fishlog_be.domain.collection.dto;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 내 인증 기록 전체 응답 — 도감 인증과 도감 외 등록을 <b>합쳐 최신순으로</b> 나열한 목록.
 *
 * <p>수 세 개를 함께 담는 이유: 목록 위에 "총 12건"을 띄우는 것 외에, 두 종류의 비율이 화면 분기에 쓰인다. 도감 외 기록만 있는 사용자에게 "도감을 채워보세요"를
 * 띄우는 식이다. 프론트가 {@code records}를 순회해 세지 않아도 되도록 서버가 함께 준다.
 *
 * <p><b>{@link MyDexResponse}·{@link MyCustomDexResponse}와 세는 단위가 다르다.</b> 그쪽은 <b>어종</b> 수(칸 수)를
 * 세지만 여기는 <b>기록</b> 수를 센다 — 감성돔을 세 번 잡았다면 도감 그리드에는 칸 1개, 이 목록에는 줄 3개다.
 */
@Schema(title = "CatchHistoryResponse DTO", description = "내 인증 기록 전체(도감 + 도감 외, 최신순)")
public record CatchHistoryResponse(
    @Schema(description = "전체 기록 수(= records 배열 길이)", example = "12") int totalCount,
    @Schema(description = "그중 도감 인증 기록 수(recordType DEX)", example = "9") int dexCount,
    @Schema(description = "그중 도감 외 수기 등록 기록 수(recordType CUSTOM)", example = "3") int customCount,
    @Schema(description = "기록 목록(최신순). 기록이 없으면 빈 배열") List<CatchHistoryEntryResponse> records) {

  /**
   * 이미 최신순으로 정렬된 목록을 감싼다. 정렬은 호출부(서비스)가 {@link CatchHistoryEntryResponse#LATEST_FIRST}로 끝내 두며, 여기서
   * 다시 정렬하지 않는다 — 정렬 책임이 두 곳에 있으면 한쪽만 바뀌었을 때 어느 쪽이 이겼는지 알 수 없다.
   */
  public static CatchHistoryResponse of(List<CatchHistoryEntryResponse> records) {
    int dexCount =
        (int) records.stream().filter(r -> r.recordType() == CatchRecordType.DEX).count();
    return new CatchHistoryResponse(records.size(), dexCount, records.size() - dexCount, records);
  }
}
