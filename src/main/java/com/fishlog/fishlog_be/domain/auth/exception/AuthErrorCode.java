package com.fishlog.fishlog_be.domain.auth.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 인증(회원가입/로그인) 도메인 에러 코드. 접두사 {@code A}. → docs/security.md §5 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {
  EMAIL_ALREADY_EXISTS("A001", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT),
  EMAIL_NOT_VERIFIED("A002", "이메일 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST),
  VERIFICATION_CODE_EXPIRED("A003", "인증코드가 만료되었거나 발급되지 않았습니다.", HttpStatus.BAD_REQUEST),
  VERIFICATION_CODE_MISMATCH("A004", "인증코드가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
  NICKNAME_ALREADY_EXISTS("A005", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
  INVALID_CREDENTIALS("A006", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
  INVALID_REFRESH_TOKEN("A007", "유효하지 않은 refresh 토큰입니다.", HttpStatus.UNAUTHORIZED),
  EMAIL_DOMAIN_NOT_ALLOWED("A008", "허용되지 않은 이메일 도메인입니다.", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
