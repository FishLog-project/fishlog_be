package com.fishlog.fishlog_be.global.s3;

import com.fishlog.fishlog_be.global.exception.CustomException;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

  private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
  private static final String HOST_MARKER = ".amazonaws.com/";

  private final S3Client s3Client;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${spring.cloud.aws.region.static}") // 응답 URL 문자열 조립
  private String region;

  @Override
  public String upload(MultipartFile file, PathName pathName) {
    validate(file);

    String key = pathName.getPath() + UUID.randomUUID() + extension(file.getOriginalFilename());
    try {
      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(key)
              .contentType(file.getContentType())
              .contentLength(file.getSize())
              .build();
      s3Client.putObject(
          request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    } catch (IOException e) {
      log.error("S3 업로드 실패: key={}, {}", key, e.getMessage());
      throw new CustomException(S3ErrorCode.UPLOAD_FAILED);
    }
    return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
  }

  @Override
  public void delete(String url) {
    int idx = url.indexOf(HOST_MARKER);
    if (idx < 0) {
      return; // S3 URL 형식이 아니면 무시
    }
    String key = url.substring(idx + HOST_MARKER.length());
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  private void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new CustomException(S3ErrorCode.EMPTY_FILE);
    }
    if (file.getSize() > MAX_SIZE) {
      throw new CustomException(S3ErrorCode.FILE_SIZE_EXCEEDED);
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new CustomException(S3ErrorCode.INVALID_FILE_TYPE);
    }
  }

  private String extension(String originalFilename) {
    if (originalFilename == null) {
      return "";
    }
    int dot = originalFilename.lastIndexOf('.');
    return dot >= 0 ? originalFilename.substring(dot) : "";
  }
}
