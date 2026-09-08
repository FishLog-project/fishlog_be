package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchHistoryEntryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordDetailResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CustomCatchDetailResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CustomCatchResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyCustomDexResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 도감 외 어종(사용자가 어종명을 직접 적어 등록하는 물고기) 서비스.
 *
 * <p>{@link CollectionService}와 도메인은 같지만 다루는 테이블({@code custom_fish}·{@code custom_catch_record})과
 * 규칙이 다르다 — 어종이 공통 도감이 아니라 <b>사용자별 카탈로그</b>이고, 랭킹·도감 완성도 집계에 포함되지 않는다.
 *
 * <p>조회 API 구성은 도감과 짝을 이룬다: 전체 목록 {@link #getMyCustomDex(Long)} ↔ {@code GET /api/collections/dex},
 * 어종 상세 {@link #getMyCustomCatch(Long, Long)} ↔ {@code GET /api/collections?fishId=}. →
 * docs/spec.md
 */
public interface CustomCatchService {

  /**
   * 도감 24종에 없는 물고기를 사진 + 수기 입력(어종명·서식지·크기·위치)으로 등록한다.
   *
   * <p>AI 분류를 타지 않는다 — 모델도 도감 24종만 알기 때문에 그 밖의 물고기에는 어차피 쓸 수 있는 후보를 못 준다. 그래서 이름은 전적으로 사용자가 정한다.
   *
   * <p><b>어종은 이름으로 찾아 없으면 만든다(find-or-create).</b> 같은 이름으로 다시 등록하면 새 어종이 생기는 대신 기존 어종에 기록이 한 건 붙고,
   * 서식지를 이번에 적었다면 어종의 서식지가 그 값으로 갱신된다(마지막에 적은 값이 이긴다).
   *
   * @param userId 로그인 사용자 id(컨트롤러가 JWT 토큰에서 획득해 전달)
   * @param fishName 사용자가 직접 입력한 어종명(필수, 앞뒤 공백 제거 후 최대 30자)
   * @param habitat 사용자가 직접 입력한 주요 서식지(선택, 최대 20자). 도감 어종은 시드가 채우지만 도감 밖 어종은 채워 줄 시드가 없어 잡은 사람이 적는다
   * @param size 잡은 크기(cm, 필수 — 0 초과 300 이하)
   * @param location 잡은 위치 수기 입력(선택). {@code null}·공백이면 위치 없이 기록한다
   * @param image 사진(S3 {@code custom-fish/} 경로에 저장)
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 입력 위반 {@code
   *     CollectionErrorCode}(400), 이미 도감에 있는 어종명이면 {@code FISH_ALREADY_IN_DEX}(400), 업로드 실패 {@code
   *     S3ErrorCode}
   */
  CustomCatchResponse register(
      Long userId,
      String fishName,
      String habitat,
      Double size,
      String location,
      MultipartFile image);

  /**
   * 내가 등록한 도감 외 어종 <b>전체 목록</b>을 조회한다({@code GET /api/collections/dex}와 같은 자리).
   *
   * <p>어종당 한 칸이며, 같은 이름으로 여러 번 등록했어도 칸은 하나이고 {@code catchCount}가 그 수를 담는다. 칸마다 대표 이미지(가장 최근
   * 사진)·서식지·최대 크기가 함께 온다.
   *
   * @param userId 로그인 사용자 id(컨트롤러가 JWT 토큰에서 획득해 전달)
   * @return 최근에 잡은 어종부터 정렬된 목록. 등록한 기록이 없으면 예외가 아니라 빈 목록 + 0건
   */
  MyCustomDexResponse getMyCustomDex(Long userId);

  /**
   * 도감 외 어종 <b>한 종</b>의 내 기록 상세를 조회한다({@code GET /api/collections?fishId=}와 같은 자리).
   *
   * <p>서식지 + 잡은 총 횟수 + 최대 크기 + 최근 사진(최신순 최대 4장, 각 사진에 그때의 크기·위치)으로 도감 상세와 같은 스펙이다.
   *
   * @param userId 로그인 사용자 id(컨트롤러가 JWT 토큰에서 획득해 전달)
   * @param customFishId 조회할 도감 외 어종 id(전체 목록 응답의 {@code fishes[].id})
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 어종이 없거나 <b>다른 사용자의 어종</b>이면
   *     {@code CUSTOM_FISH_NOT_FOUND}(404). 두 경우를 구분해 알려주지 않는다
   */
  CustomCatchDetailResponse getMyCustomCatch(Long userId, Long customFishId);

  /**
   * 내가 등록한 도감 외 어종 기록을 <b>기록 단위</b>로 전부 반환한다(최신순).
   *
   * <p>{@link #getMyCustomDex(Long)}이 어종당 한 칸으로 접어 주는 것과 달리, 이쪽은 접지 않은 기록 하나하나다. 같은 이름으로 세 번 등록했으면
   * 세 줄이 나온다.
   *
   * <p><b>이 도메인의 조회 API가 아니라 {@link CollectionService#getMyCatchHistory(Long)}의 재료</b>다. 통합 목록은 도감
   * 인증 기록과 합쳐 정렬해야 하는데, 그 병합을 여기서 하면 이 서비스가 {@code catch_record}까지 알아야 한다. 테이블 소유권을 지키기 위해 <b>자기
   * 테이블 몫만 응답 DTO 로 변환해 넘기고</b>, 합치고 정렬하는 일은 호출부가 맡는다.
   *
   * @param userId 로그인 사용자 id
   * @return 최신순 기록 목록. 없으면 빈 목록
   */
  List<CatchHistoryEntryResponse> getMyCustomHistory(Long userId);

  /**
   * 도감 외 어종 기록 <b>1건</b>의 상세를 조회한다({@code recordType}이 {@code CUSTOM}인 기록).
   *
   * <p>{@link #getMyCustomCatch(Long, Long)}과 인자 이름이 비슷하지만 <b>가리키는 대상이 다르다</b> — 저쪽은 어종({@code
   * custom_fish}) id 로 그 어종의 기록 전체를 요약하고, 이쪽은 기록({@code custom_catch_record}) id 로 그 한 건만 본다.
   *
   * @param userId 로그인 사용자 id
   * @param recordId 조회할 기록 id(통합 목록 응답의 {@code recordId})
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 기록이 없거나 <b>다른 사용자의 기록</b>이면
   *     {@code CATCH_RECORD_NOT_FOUND}(404). 두 경우를 구분해 알려주지 않는다
   */
  CatchRecordDetailResponse getMyCustomRecord(Long userId, Long recordId);

  /**
   * 회원탈퇴 등으로 해당 사용자의 도감 외 어종 기록과 어종 목록을 모두 삭제한다.
   *
   * <p>탈퇴 흐름은 {@link CollectionService#deleteMyRecords(Long)} 하나만 호출하고, 그쪽에서 이 메서드로 위임한다 — 정리해야 할
   * 테이블이 늘 때마다 user 도메인이 호출 목록을 따라 늘리지 않아도 되도록 collection 도메인 안에서 마무리한다.
   *
   * @param userId 삭제 대상 사용자 id
   */
  void deleteMyRecords(Long userId);
}
