package com.fishlog.fishlog_be.global.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
  /**
   * 업로드 허용 최대 이미지 크기(byte).
   *
   * <p>어종 분류(AI) 경로도 같은 한도를 사용한다 — 분류 한도가 저장 한도보다 느슨하면 "분류는 성공했는데 인증 저장이 실패"하는 흐름이 생긴다. 한 곳에서 관리해
   * 분류에 성공한 사진은 반드시 저장도 가능하게 맞춘다. 값은 모델 서버 계약 상한(10MB)에 맞췄다.
   *
   * <p><b>주의:</b> 실제 상한은 앞단 리버스 프록시가 결정한다(nginx {@code client_max_body_size}, 미설정 시 기본 1MB). 이 상수만
   * 올리고 프록시를 그대로 두면 요청이 Spring 에 닿지도 못한다 → docs/media.md "크기 한도".
   */
  long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

  /**
   * 이미지 파일을 S3에 업로드하고 접근 URL을 반환한다.
   *
   * @param file 업로드 파일(이미지, 최대 10MB)
   * @param pathName 저장 경로 prefix
   * @return 업로드된 객체의 URL
   */
  String upload(MultipartFile file, PathName pathName);

  /** URL로 식별되는 S3 객체를 삭제한다. */
  void delete(String url);
}
