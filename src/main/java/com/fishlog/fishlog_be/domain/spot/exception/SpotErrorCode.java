package com.fishlog.fishlog_be.domain.spot.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 낚시 스팟 도메인 에러 코드. 접두사 {@code S}. → docs/spec.md */
@Getter
@AllArgsConstructor
public enum SpotErrorCode implements BaseErrorCode {
  SPOT_NOT_FOUND("S001", "스팟을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
