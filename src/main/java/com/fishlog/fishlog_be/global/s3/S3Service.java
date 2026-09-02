package com.fishlog.fishlog_be.global.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

  /**
   * 업로드 허용 최대 이미지 크기(byte).
   *
   * <p>어종 분류(AI) 경로도 같은 한도를 사용한다 — 분류 한도(모델 계약 10MB)가 저장 한도보다 느슨하면 "분류는 성공했는데 인증 저장이 실패"하는 흐름이 생긴다.
   * 한 곳에서 관리해 분류에 성공한 사진은 반드시 저장도 가능하게 맞춘다.
   */
  long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

  /**
   * 이미지 파일을 S3에 업로드하고 접근 URL을 반환한다.
   *
   * @param file 업로드 파일(이미지, 최대 5MB)
   * @param pathName 저장 경로 prefix
   * @return 업로드된 객체의 URL
   */
  String upload(MultipartFile file, PathName pathName);

  /** URL로 식별되는 S3 객체를 삭제한다. */
  void delete(String url);
}
