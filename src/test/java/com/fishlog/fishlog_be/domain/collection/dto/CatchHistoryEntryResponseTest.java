package com.fishlog.fishlog_be.domain.collection.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecordType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 통합 목록의 정렬 규칙. 핵심은 "두 테이블에서 온 기록이 섞여도 순서가 최신순이고, <b>같은 입력이면 언제나 같은 순서</b>"라는 것이다 — 새로고침할 때마다 목록이
 * 흔들리면 안 된다.
 */
class CatchHistoryEntryResponseTest {

  private static CatchHistoryEntryResponse entry(
      Long recordId, CatchRecordType type, String recordedAt) {
    return new CatchHistoryEntryResponse(
        recordId,
        type,
        1L,
        "감성돔",
        31.0,
        "https://.../uuid.jpg",
        null,
        LocalDateTime.parse(recordedAt));
  }

  @Test
  @DisplayName("두 테이블에서 온 기록을 합쳐도 recordedAt 내림차순(최신순)으로 정렬된다")
  void sortsLatestFirstAcrossTables() {
    List<CatchHistoryEntryResponse> entries =
        new ArrayList<>(
            List.of(
                entry(41L, CatchRecordType.DEX, "2026-09-01T09:11:47"),
                entry(7L, CatchRecordType.CUSTOM, "2026-09-05T14:32:10"),
                entry(42L, CatchRecordType.DEX, "2026-09-06T18:20:04")));

    entries.sort(CatchHistoryEntryResponse.LATEST_FIRST);

    // id 가 아니라 시각이 기준이다 — CUSTOM 7번이 DEX 41번보다 위로 온다.
    assertThat(entries)
        .extracting(CatchHistoryEntryResponse::recordId)
        .containsExactly(42L, 7L, 41L);
  }

  @Test
  @DisplayName("recordedAt 이 같아도 순서가 결정적이다(종류 → id 내림차순)")
  void breaksTiesDeterministically() {
    String sameMoment = "2026-09-06T18:20:04";
    List<CatchHistoryEntryResponse> ascending =
        new ArrayList<>(
            List.of(
                entry(5L, CatchRecordType.CUSTOM, sameMoment),
                entry(9L, CatchRecordType.CUSTOM, sameMoment),
                entry(9L, CatchRecordType.DEX, sameMoment),
                entry(12L, CatchRecordType.DEX, sameMoment)));
    // 입력 순서만 다른 같은 집합. 정렬 결과가 같아야 "매번 같은 순서"가 성립한다.
    List<CatchHistoryEntryResponse> shuffled = new ArrayList<>(ascending.reversed());

    ascending.sort(CatchHistoryEntryResponse.LATEST_FIRST);
    shuffled.sort(CatchHistoryEntryResponse.LATEST_FIRST);

    assertThat(ascending).isEqualTo(shuffled);
    // DEX(enum 선언 순서상 앞) 가 먼저, 각 종류 안에서는 id 내림차순.
    assertThat(ascending)
        .extracting(CatchHistoryEntryResponse::recordType, CatchHistoryEntryResponse::recordId)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(CatchRecordType.DEX, 12L),
            org.assertj.core.groups.Tuple.tuple(CatchRecordType.DEX, 9L),
            org.assertj.core.groups.Tuple.tuple(CatchRecordType.CUSTOM, 9L),
            org.assertj.core.groups.Tuple.tuple(CatchRecordType.CUSTOM, 5L));
  }

  @Test
  @DisplayName("응답 래퍼는 종류별 개수를 세어 함께 내려준다")
  void countsByType() {
    CatchHistoryResponse response =
        CatchHistoryResponse.of(
            List.of(
                entry(42L, CatchRecordType.DEX, "2026-09-06T18:20:04"),
                entry(7L, CatchRecordType.CUSTOM, "2026-09-05T14:32:10"),
                entry(41L, CatchRecordType.DEX, "2026-09-01T09:11:47")));

    assertThat(response.totalCount()).isEqualTo(3);
    assertThat(response.dexCount()).isEqualTo(2);
    assertThat(response.customCount()).isEqualTo(1);
    // 세 값의 관계가 화면 분기의 근거라 합이 어긋나면 안 된다.
    assertThat(response.dexCount() + response.customCount()).isEqualTo(response.totalCount());
  }

  @Test
  @DisplayName("기록이 없으면 예외가 아니라 빈 목록 + 0건이다")
  void emptyIsNotAnError() {
    CatchHistoryResponse response = CatchHistoryResponse.of(List.of());

    assertThat(response.totalCount()).isZero();
    assertThat(response.dexCount()).isZero();
    assertThat(response.customCount()).isZero();
    assertThat(response.records()).isEmpty();
  }
}
