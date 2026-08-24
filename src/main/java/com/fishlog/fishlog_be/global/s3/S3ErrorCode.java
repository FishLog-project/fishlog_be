package com.fishlog.fishlog_be.global.s3;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum S3ErrorCode implements BaseErrorCode {
  EMPTY_FILE("S001", "업로드할 파일이 없습니다.", HttpStatus.BAD_REQUEST),
  INVALID_FILE_TYPE("S002", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
  FILE_SIZE_EXCEEDED("S003", "파일 크기는 5MB 이하여야 합니다.", HttpStatus.BAD_REQUEST),
  UPLOAD_FAILED("S004", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
