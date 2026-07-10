package com.fishlog.fishlog_be.global.exception.model;

import org.springframework.http.HttpStatus;

public interface BaseErrorCode {
  String getCode();

  String getMessage();

  HttpStatus getStatus();
}
