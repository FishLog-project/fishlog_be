package com.fishlog.fishlog_be.domain.fish.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 어종(도감) 도메인 에러 코드. 접두사 {@code F}. */
@Getter
@AllArgsConstructor
public enum FishErrorCode implements BaseErrorCode {
  FISH_NOT_FOUND("F001", "해당 어종을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
