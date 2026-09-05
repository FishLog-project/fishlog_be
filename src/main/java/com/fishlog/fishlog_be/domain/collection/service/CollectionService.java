package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.ClassifyResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.dto.VerifyResponse;
import org.springframework.web.multipart.MultipartFile;

/** 사용자 도감(어종 인증) 조회 서비스. */
public interface CollectionService {

  /**
   * 로그인 사용자가 특정 어종을 인증한 기록 요약(서식지 + 잡은 총 횟수 + 최근 인증 사진)을 조회한다.
   *
   * <p>사진은 <b>최신순 최대 4장</b>으로 자르지만 {@code catchCount}는 자르지 않은 전체 횟수다 — 화면이 썸네일 4칸만 그리기 때문이며, "더
   * 있다"는 사실은 두 값의 차이로 남는다. 각 사진 항목에 그때 기록한 크기·위치가 함께 담겨 오버레이에 추가 조회가 필요 없다.
   *
   * @param userId 로그인 사용자 id(컨트롤러가 JWT 토큰에서 획득해 전달)
   * @param fishId 전체 도감 어종 id
   * @return 안 잡았어도 예외가 아니라 catchCount 0 · 빈 목록(서식지는 채워짐)
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 도감에 없는 어종이면 {@code
   *     FISH_NOT_FOUND}(404). 서식지를 채우려 어종을 조회하기 때문이며, "안 잡은 어종"(200)과는 다르다
   */
  CatchRecordResponse getMyCatch(Long userId, Long fishId);

  /**
   * 내 도감 그리드용 조회. 전체 수집 대상 어종을 순서대로 반환하되, 각 어종에 대해 {@code userId}가 잡았는지({@code caught})를 표시한다.
   *
   * <p>UI가 칸마다 이미지/그림자를 분기하도록 하기 위한 단일 조회다. 어종 목록은 전체 도감과 동일한 순서·집합이며, 잡은 어종 집합만 덧입힌다.
   *
   * @param userId 로그인 사용자 id(컨트롤러가 JWT 토큰에서 획득해 전달)
   * @return 총 수·잡은 수·어종 목록(각 항목에 caught 포함)
   */
  MyDexResponse getMyDex(Long userId);

  /**
   * 회원탈퇴 등으로 해당 사용자의 모든 도감 기록을 삭제한다(도감 인증 + 도감 외 어종).
   *
   * <p>다른 도메인(user)의 회원탈퇴 흐름에서 도메인 경계를 지켜 호출하기 위한 진입점이다. 정리 대상 테이블이 늘어도 <b>호출부는 이 메서드 하나만</b> 알면
   * 되도록, 도감 외 어종({@code custom_catch_record}) 정리는 여기서 {@link CustomCatchService}로 위임한다.
   *
   * @param userId 삭제 대상 사용자 id
   */
  void deleteMyRecords(Long userId);

  /**
   * 사진으로 어종 후보(Top-3)를 분류한다. <b>저장하지 않는 순수 조회</b>다 — S3에도, DB에도 아무것도 쓰지 않는다.
   *
   * <p>사용자가 후보 중 하나를 고른 뒤 {@link #verify(Long, Long, Double, String, MultipartFile)}로 실제 인증을 완료한다.
   * 분류와 저장을 나눈 이유는 Top-1 정확도가 81%뿐이라 자동 확정이 5건 중 1건꼴로 도감을 오염시키기 때문이다(Top-3는 90.7%).
   *
   * @param image 원본 이미지(리사이즈·재인코딩 없이 모델로 전달)
   * @return 후보 목록. 모델이 확신하지 못해도({@code uncertain}) 후보는 그대로 담긴다
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 사진 자체가 문제면 {@code AiErrorCode}의
   *     4xx, 모델 서버에 닿지 못하면 {@code CLASSIFY_UNAVAILABLE}(503)
   */
  ClassifyResponse classify(MultipartFile image);

  /**
   * 어종 인증을 완료해 도감에 기록한다(인증 1건 = {@code catch_record} 1행).
   *
   * <p>어종은 <b>모델이 아니라 사용자가 확정</b>한다 — 분류 후보에서 골랐든, 24종 밖 어종이라 목록에서 직접 골랐든 이 메서드는 동일하게 동작한다. 덕분에 모델
   * 서버가 죽어도 인증 기능 자체는 살아 있다.
   *
   * @param userId 로그인 사용자 id(컨트롤러가 JWT 토큰에서 획득해 전달)
   * @param fishId 사용자가 확정한 어종 id
   * @param size 잡은 크기(cm, 필수 — 크기 랭킹 기준)
   * @param location 잡은 위치 수기 입력(선택). {@code null}·공백이면 위치 없이 기록하며, 앞뒤 공백은 제거해 저장한다
   * @param image 인증 사진(S3 {@code fish/} 경로에 저장)
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 어종 미존재 {@code
   *     FISH_NOT_FOUND}(404), 크기 이상·위치 길이 초과 {@code CollectionErrorCode}(400), 업로드 실패 {@code
   *     S3ErrorCode}
   */
  VerifyResponse verify(
      Long userId, Long fishId, Double size, String location, MultipartFile image);
}
