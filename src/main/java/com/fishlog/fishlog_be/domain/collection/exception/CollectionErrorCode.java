package com.fishlog.fishlog_be.domain.collection.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 사용자 도감(어종 인증) 에러 코드.
 *
 * <p>어종이 없는 경우는 이 enum이 아니라 fish 도메인의 {@code FishErrorCode.FISH_NOT_FOUND(F001)}를 그대로 쓴다 — 같은 사실을 두
 * 코드로 표현하지 않기 위해서다. 사진 관련 실패는 {@code AiErrorCode}(분류)·{@code S3ErrorCode}(저장)가 담당한다.
 *
 * <p>{@code C004}~{@code C008}은 도감 외 어종({@code POST /api/collections/custom}) 전용이다. {@code C006}은
 * "이름이 틀렸다"가 아니라 <b>다른 엔드포인트로 가라</b>는 안내에 가깝다 — 도감에 있는 어종을 기타 어종으로 등록하면 도감 칸도 안 채워지고 랭킹에도 안 잡히므로,
 * 저장에 성공시키는 대신 400으로 끊어 도감 인증({@code /verify})으로 유도한다.
 *
 * <p>{@code C008}은 <b>남의 어종을 조회한 경우도 포함</b>한다. 소유자가 다르면 403이 아니라 404 로 답해 어종의 존재 여부 자체를 알려주지 않는다.
 */
@Getter
@AllArgsConstructor
public enum CollectionErrorCode implements BaseErrorCode {
  INVALID_SIZE("C001", "어종 크기(cm)는 0보다 커야 합니다.", HttpStatus.BAD_REQUEST),
  SIZE_OUT_OF_RANGE("C002", "어종 크기(cm)가 현실적인 범위를 벗어났습니다.", HttpStatus.BAD_REQUEST),
  LOCATION_TOO_LONG("C003", "잡은 위치는 100자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST),
  INVALID_FISH_NAME("C004", "어종명을 입력해주세요.", HttpStatus.BAD_REQUEST),
  FISH_NAME_TOO_LONG("C005", "어종명은 30자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST),
  FISH_ALREADY_IN_DEX("C006", "이미 도감에 있는 어종입니다. 어종 인증으로 등록해주세요.", HttpStatus.BAD_REQUEST),
  HABITAT_TOO_LONG("C007", "주요 서식지는 20자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST),
  CUSTOM_FISH_NOT_FOUND("C008", "등록한 도감 외 어종을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
