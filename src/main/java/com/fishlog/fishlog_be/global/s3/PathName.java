package com.fishlog.fishlog_be.global.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** S3 업로드 경로 prefix. */
@Getter
@RequiredArgsConstructor
public enum PathName {
  PROFILE("profile/"),
  FISH("fish/");

  private final String path;
}