package com.fishlog.fishlog_be.global.s3;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

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