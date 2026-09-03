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
 */
@Getter
@AllArgsConstructor
public enum CollectionErrorCode implements BaseErrorCode {
  INVALID_SIZE("C001", "어종 크기(cm)는 0보다 커야 합니다.", HttpStatus.BAD_REQUEST),
  SIZE_OUT_OF_RANGE("C002", "어종 크기(cm)가 현실적인 범위를 벗어났습니다.", HttpStatus.BAD_REQUEST),
  LOCATION_TOO_LONG("C003", "잡은 위치는 100자 이하로 입력해주세요.", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
