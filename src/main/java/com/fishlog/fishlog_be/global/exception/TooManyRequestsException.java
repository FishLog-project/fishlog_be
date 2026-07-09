package com.fishlog.fishlog_be.global.exception;

import lombok.Getter;

/**
 * 요청 빈도 제한(429) 초과 예외. 클라이언트 재시도 UI를 위해 남은 대기 시간({@code retryAfterSec})을 함께 전달하며, {@link
 * GlobalExceptionHandler}가 이를 응답 {@code data.retryAfterSec}로 변환한다.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

  private final long retryAfterSec;

  public TooManyRequestsException(String message, long retryAfterSec) {
    super(message);
    this.retryAfterSec = retryAfterSec;
  }
}
