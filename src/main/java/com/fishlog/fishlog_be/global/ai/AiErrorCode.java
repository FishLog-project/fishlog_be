package com.fishlog.fishlog_be.global.ai;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 어종 분류(AI) 연동 에러 코드. AI001~AI005는 모델 서버가 4xx로 돌려주는 <b>입력 문제</b>(재시도해도 같은 결과), AI006~AI007은 모델 서버 쪽
 * <b>가용성 문제</b>다. → docs/external.md §2
 *
 * <p>가용성 실패(AI007)는 "목록에서 직접 선택" 대안 경로로 유도하는 메시지를 담는다 — 24종 밖 어종(향어·학꽁치 등)도 같은 경로를 쓴다.
 */
@Getter
@AllArgsConstructor
public enum AiErrorCode implements BaseErrorCode {
  EMPTY_FILE("AI001", "인증할 사진이 없습니다.", HttpStatus.BAD_REQUEST),
  INVALID_FILE_TYPE("AI002", "이미지 파일만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST),
  IMAGE_DECODE_FAILED("AI003", "사진을 읽을 수 없습니다. 다른 사진으로 다시 시도해주세요.", HttpStatus.BAD_REQUEST),
  UNSUPPORTED_FORMAT(
      "AI004", "지원하지 않는 이미지 형식입니다. JPG 또는 PNG로 올려주세요.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  FILE_TOO_LARGE("AI005", "사진 크기는 5MB 이하여야 합니다.", HttpStatus.PAYLOAD_TOO_LARGE),
  IMAGE_TOO_LARGE("AI006", "사진 해상도가 너무 큽니다. 더 작은 사진으로 시도해주세요.", HttpStatus.PAYLOAD_TOO_LARGE),
  MODEL_NOT_LOADED("AI007", "어종 분류 서버가 준비 중입니다. 잠시 후 다시 시도해주세요.", HttpStatus.SERVICE_UNAVAILABLE),
  CLASSIFY_UNAVAILABLE(
      "AI008", "어종 분류 서버에 연결할 수 없습니다. 목록에서 직접 선택해주세요.", HttpStatus.SERVICE_UNAVAILABLE);

  private final String code;
  private final String message;
  private final HttpStatus status;

  /**
   * 모델 서버 실패 응답의 {@code error} 코드를 우리 에러 코드로 옮긴다. 모르는 코드(모델 서버가 새 코드를 추가한 경우)는 분류 불가로 보고 {@link
   * #CLASSIFY_UNAVAILABLE}로 떨어뜨린다.
   */
  public static AiErrorCode fromModelError(String modelError) {
    if (modelError == null) {
      return CLASSIFY_UNAVAILABLE;
    }
    return switch (modelError) {
      case "EMPTY_FILE" -> EMPTY_FILE;
      case "IMAGE_DECODE_FAILED" -> IMAGE_DECODE_FAILED;
      case "UNSUPPORTED_FORMAT" -> UNSUPPORTED_FORMAT;
      case "FILE_TOO_LARGE" -> FILE_TOO_LARGE;
      case "IMAGE_TOO_LARGE" -> IMAGE_TOO_LARGE;
      case "MODEL_NOT_LOADED" -> MODEL_NOT_LOADED;
      default -> CLASSIFY_UNAVAILABLE;
    };
  }
}
