package com.fishlog.fishlog_be.domain.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fishlog.fishlog_be.domain.collection.dto.CatchHistoryEntryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CatchHistoryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordDetailResponse;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecordType;
import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.global.ai.FishClassifyClient;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.s3.S3Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 통합 기록 조회({@code /api/collections/records})의 서비스 동작.
 *
 * <p>검증하려는 핵심은 세 가지다 — ① 두 테이블의 기록을 <b>합쳐서</b> 최신순으로 준다, ② 단건 조회는 {@code type}으로 <b>어느 테이블을 볼지</b>
 * 가른다, ③ 없는 기록·남의 기록은 예외 없이 <b>같은 404</b>로 수렴한다.
 */
@ExtendWith(MockitoExtension.class)
class CollectionHistoryServiceTest {

  private static final Long USER_ID = 1L;

  @Mock private CatchRecordRepository catchRecordRepository;
  @Mock private FishService fishService;
  @Mock private FishClassifyClient fishClassifyClient;
  @Mock private S3Service s3Service;
  @Mock private CustomCatchService customCatchService;

  @InjectMocks private CollectionServiceImpl collectionService;

  private static Fish fish(Long id, String name, String habitat) {
    return Fish.builder().id(id).name(name).habitat(habitat).build();
  }

  private static CatchRecord dexRecord(Long id, Fish fish, Double size, String recordedAt) {
    return CatchRecord.builder()
        .id(id)
        .userId(USER_ID)
        .fish(fish)
        .certifiedImageUrl("https://.../fish/uuid.jpg")
        .size(size)
        .catchLocation("격포항 방파제")
        .createdAt(LocalDateTime.parse(recordedAt))
        .build();
  }

  private static CatchHistoryEntryResponse customEntry(Long id, String name, String recordedAt) {
    return new CatchHistoryEntryResponse(
        id,
        CatchRecordType.CUSTOM,
        3L,
        name,
        41.0,
        "https://.../custom-fish/uuid.jpg",
        "한탄강 고석정",
        LocalDateTime.parse(recordedAt));
  }

  @Test
  @DisplayName("도감 기록과 도감 외 기록을 합쳐 최신순 한 목록으로 준다")
  void mergesBothSourcesLatestFirst() {
    when(catchRecordRepository.findAllWithFishByUserId(USER_ID))
        .thenReturn(
            List.of(
                dexRecord(42L, fish(1L, "감성돔", "바다"), 31.0, "2026-09-06T18:20:04"),
                dexRecord(41L, fish(4L, "참돔", "바다"), 22.5, "2026-09-01T09:11:47")));
    when(customCatchService.getMyCustomHistory(USER_ID))
        .thenReturn(List.of(customEntry(7L, "쏘가리", "2026-09-05T14:32:10")));

    CatchHistoryResponse response = collectionService.getMyCatchHistory(USER_ID);

    // 도감 외 기록이 두 도감 기록 사이에 끼어든다 — 테이블이 아니라 시각이 순서를 정한다는 뜻이다.
    assertThat(response.records())
        .extracting(CatchHistoryEntryResponse::fishName)
        .containsExactly("감성돔", "쏘가리", "참돔");
    assertThat(response.totalCount()).isEqualTo(3);
    assertThat(response.dexCount()).isEqualTo(2);
    assertThat(response.customCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("기록이 하나도 없어도 예외가 아니라 빈 목록이다")
  void emptyHistoryIsNotAnError() {
    when(catchRecordRepository.findAllWithFishByUserId(USER_ID)).thenReturn(List.of());
    when(customCatchService.getMyCustomHistory(USER_ID)).thenReturn(List.of());

    CatchHistoryResponse response = collectionService.getMyCatchHistory(USER_ID);

    assertThat(response.records()).isEmpty();
    assertThat(response.totalCount()).isZero();
  }

  @Test
  @DisplayName("type=DEX 는 도감 기록을 조회하고 어종의 서식지를 함께 준다")
  void findsDexRecord() {
    when(catchRecordRepository.findWithFishByIdAndUserId(42L, USER_ID))
        .thenReturn(
            Optional.of(dexRecord(42L, fish(1L, "감성돔", "바다"), 31.0, "2026-09-06T18:20:04")));

    CatchRecordDetailResponse response =
        collectionService.getMyCatchRecord(USER_ID, 42L, CatchRecordType.DEX);

    assertThat(response.recordId()).isEqualTo(42L);
    assertThat(response.recordType()).isEqualTo(CatchRecordType.DEX);
    assertThat(response.fishName()).isEqualTo("감성돔");
    assertThat(response.habitat()).isEqualTo("바다"); // 기록이 아니라 어종에서 온 값
    // 도감 기록은 형제 서비스를 거치지 않는다(type 이 실제로 경로를 가르는지 확인).
    verifyNoInteractions(customCatchService);
  }

  @Test
  @DisplayName("type=CUSTOM 은 도감 외 기록 서비스로 위임한다")
  void delegatesCustomRecord() {
    CatchRecordDetailResponse delegated =
        new CatchRecordDetailResponse(
            7L,
            CatchRecordType.CUSTOM,
            3L,
            "쏘가리",
            "강",
            41.0,
            "https://.../custom-fish/uuid.jpg",
            "한탄강 고석정",
            LocalDateTime.parse("2026-09-05T14:32:10"));
    when(customCatchService.getMyCustomRecord(USER_ID, 7L)).thenReturn(delegated);

    CatchRecordDetailResponse response =
        collectionService.getMyCatchRecord(USER_ID, 7L, CatchRecordType.CUSTOM);

    assertThat(response).isEqualTo(delegated);
    verify(customCatchService).getMyCustomRecord(USER_ID, 7L);
    // 도감 테이블은 쳐다보지도 않는다.
    verify(catchRecordRepository, org.mockito.Mockito.never())
        .findWithFishByIdAndUserId(any(), any());
  }

  @Test
  @DisplayName("없는 기록·남의 기록은 모두 C009(404)로 수렴한다")
  void missingOrForeignRecordIsNotFound() {
    // 소유자 조건이 쿼리에 들어 있어, 남의 기록도 "없는 기록"과 똑같이 빈 결과로 돌아온다.
    when(catchRecordRepository.findWithFishByIdAndUserId(999L, USER_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> collectionService.getMyCatchRecord(USER_ID, 999L, CatchRecordType.DEX))
        .isInstanceOf(CustomException.class)
        .hasMessageContaining("인증 기록을 찾을 수 없습니다.");
  }
}
