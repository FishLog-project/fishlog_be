package com.fishlog.fishlog_be.global.ai;

import com.fishlog.fishlog_be.global.ai.dto.PredictResponse;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.s3.S3Service;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link FishClassifyClient} 구현. multipart 필드명 {@code file}로 원본 바이트를 그대로 전송한다. → docs/external.md
 * §2
 *
 * <p><b>재시도 정책:</b> 4xx는 입력이 잘못된 것이라 다시 보내도 같은 답이므로 재시도하지 않는다. 5xx·타임아웃·네트워크 오류만 1회 재시도한다(모델 평균 응답
 * 80ms, read timeout 5s라 재시도 비용이 낮다).
 *
 * <p><b>RestClient 는 빈으로 주입받아 재사용한다.</b> 요청마다 새로 만들면 커넥션 풀이 매번 버려져 처리량이 15건/초 → 3.4건/초로 떨어지는 것이
 * 실측됐다.
 */
@Component
@Slf4j
public class FishClassifyClientImpl implements FishClassifyClient {

  private static final int MAX_ATTEMPTS = 2; // 최초 1회 + 재시도 1회

  private final RestClient restClient;
  private final String predictUrl;

  public FishClassifyClientImpl(
      RestClient fishClassifyRestClient,
      @Value("${external.fish-classify.base-url}") String baseUrl) {
    this.restClient = fishClassifyRestClient;
    this.predictUrl = baseUrl + "/predict";
  }

  @Override
  public Optional<PredictResponse> predict(MultipartFile image) {
    validate(image);

    // 바이트를 한 번만 읽어 재시도에 재사용한다(MultipartFile 의 InputStream 은 재차 읽기가 보장되지 않는다).
    // 리사이즈·재인코딩 없이 원본 그대로 — 앞단에서 다시 구우면 모델 정확도가 조용히 떨어진다.
    byte[] bytes = readBytes(image);
    MultiValueMap<String, HttpEntity<?>> body = buildMultipart(image, bytes);

    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        ResponseEntity<PredictResponse> response = call(body);
        HttpStatusCode status = response.getStatusCode();
        PredictResponse payload = response.getBody();

        if (status.is2xxSuccessful() && payload != null && payload.success()) {
          return Optional.of(payload);
        }
        if (status.is4xxClientError()) {
          // 입력 문제 — 재시도해도 같다. 모델이 알려준 이유를 그대로 사용자에게 옮긴다.
          throw new CustomException(AiErrorCode.fromModelError(errorCodeOf(payload)));
        }
        // 5xx 또는 2xx인데 success=false → 재시도 대상
        lastFailure = new IllegalStateException("모델 서버 응답 실패 status=" + status.value());
      } catch (CustomException e) {
        throw e; // 4xx 매핑 결과는 그대로 위로
      } catch (RuntimeException e) {
        // 타임아웃·커넥션 거부 등(ResourceAccessException) — 재시도 대상
        lastFailure = e;
      }
      if (attempt < MAX_ATTEMPTS) {
        log.warn("어종 분류 호출 실패, 재시도합니다({}/{}): {}", attempt, MAX_ATTEMPTS, message(lastFailure));
      }
    }

    log.error("어종 분류 호출 최종 실패: {}", message(lastFailure));
    return Optional.empty();
  }

  private ResponseEntity<PredictResponse> call(MultiValueMap<String, HttpEntity<?>> body) {
    return restClient
        .post()
        .uri(predictUrl)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(body)
        .retrieve()
        // 기본 예외 변환을 끄고 상태코드·본문을 직접 판단한다(4xx 본문의 error 코드가 필요하다).
        .onStatus(HttpStatusCode::isError, (request, response) -> {})
        .toEntity(PredictResponse.class);
  }

  private MultiValueMap<String, HttpEntity<?>> buildMultipart(MultipartFile image, byte[] bytes) {
    String filename =
        image.getOriginalFilename() == null ? "catch.jpg" : image.getOriginalFilename();
    ByteArrayResource resource =
        new ByteArrayResource(bytes) {
          @Override
          public String getFilename() {
            return filename; // 파일명이 없으면 일부 서버가 파일 파트로 인식하지 못한다
          }
        };
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder
        .part("file", resource)
        .header(
            HttpHeaders.CONTENT_TYPE,
            image.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : image.getContentType());
    return builder.build();
  }

  /** 모델에 보내기 전 걸러낼 수 있는 것은 미리 거른다(빈 파일·비이미지·용량). 리사이즈가 아니라 거부다. */
  private void validate(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new CustomException(AiErrorCode.EMPTY_FILE);
    }
    // S3 저장 한도와 같은 값을 쓴다 → 분류에 성공한 사진은 반드시 인증(저장)도 가능하다.
    if (image.getSize() > S3Service.MAX_IMAGE_SIZE) {
      throw new CustomException(AiErrorCode.FILE_TOO_LARGE);
    }
    String contentType = image.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new CustomException(AiErrorCode.INVALID_FILE_TYPE);
    }
  }

  private byte[] readBytes(MultipartFile image) {
    try {
      return image.getBytes();
    } catch (IOException e) {
      log.error("업로드 이미지 읽기 실패: {}", e.getMessage());
      throw new CustomException(AiErrorCode.IMAGE_DECODE_FAILED);
    }
  }

  private String errorCodeOf(PredictResponse payload) {
    return payload == null ? null : payload.error();
  }

  private String message(RuntimeException e) {
    return e == null ? "원인 미상" : e.getClass().getSimpleName() + ": " + e.getMessage();
  }
}
