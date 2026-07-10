package com.fishlog.fishlog_be.global.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {
  INVALID_INPUT_VALUE("G001", "유효하지 않은 입력입니다.", HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND("G002", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  INTERNAL_SERVER_ERROR("G003", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  UNAUTHORIZED("G004", "로그인 인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
  FORBIDDEN("G005", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
  METHOD_NOT_ALLOWED("G006", "허용되지 않은 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
