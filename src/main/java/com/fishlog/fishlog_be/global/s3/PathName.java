package com.fishlog.fishlog_be.global.s3;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** S3 업로드 경로 prefix. */
@Getter
@RequiredArgsConstructor
public enum PathName {
  PROFILE("profile/"),
  FISH("fish/"),
  /**
   * 도감 외 어종(사용자가 어종명을 직접 적어 등록한 기록)의 사진.
   *
   * <p>도감 인증 사진({@link #FISH})과 경로를 나눈 이유: 이쪽 사진은 <b>모델·도감 어느 쪽으로도 검증되지 않은 이름</b>이 붙어 있다. 나중에 신규 어종
   * 후보를 추리거나 학습 데이터로 쓸 때, 검증된 사진과 섞여 있으면 prefix 만으로 골라낼 수 없다.
   */
  CUSTOM_FISH("custom-fish/");

  private final String path;
}
