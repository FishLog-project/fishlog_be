package com.fishlog.fishlog_be.domain.user.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 사용자(마이페이지) 도메인 에러 코드. 접두사 {@code U}. → docs/security.md */
@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
  USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  INVALID_CURRENT_PASSWORD("U002", "현재 비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
  NICKNAME_ALREADY_EXISTS("U003", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
  SAME_AS_CURRENT_PASSWORD("U004", "새 비밀번호가 현재 비밀번호와 동일합니다.", HttpStatus.BAD_REQUEST);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
