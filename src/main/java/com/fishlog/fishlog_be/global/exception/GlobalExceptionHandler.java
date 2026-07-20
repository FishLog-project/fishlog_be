package com.fishlog.fishlog_be.global.exception;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import java.sql.SQLTransientConnectionException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<BaseResponse<Object>> handleCustomException(CustomException ex) {
    BaseErrorCode errorCode = ex.getErrorCode();
    log.error("[{}] {}", errorCode.getCode(), ex.getMessage());
    return ResponseEntity.status(errorCode.getStatus())
        .body(BaseResponse.error(errorCode.getStatus().value(), ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Object>> handleValidationException(
      MethodArgumentNotValidException ex) {
    String errorMessages =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> String.format("[%s] %s", e.getField(), e.getDefaultMessage()))
            .collect(Collectors.joining(" / "));
    log.warn("Validation 오류 발생: {}", errorMessages);
    return ResponseEntity.badRequest().body(BaseResponse.error(400, errorMessages));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<BaseResponse<Object>> handleNotReadable(
      HttpMessageNotReadableException ex) {
    log.warn("요청 본문 파싱 실패: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(BaseResponse.error(400, "요청 형식이 올바르지 않습니다."));
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<BaseResponse<Object>> handleBadRequestParam(Exception ex) {
    log.warn("요청 파라미터 오류: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(BaseResponse.error(400, "요청 파라미터가 올바르지 않습니다."));
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<BaseResponse<Object>> handleTooManyRequests(TooManyRequestsException ex) {
    log.warn("요청 빈도 제한 초과: {} (retryAfterSec={})", ex.getMessage(), ex.getRetryAfterSec());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(
            new BaseResponse<>(
                false, 429, ex.getMessage(), Map.of("retryAfterSec", ex.getRetryAfterSec())));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<BaseResponse<Object>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    log.warn("허용되지 않은 메서드: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(BaseResponse.error(405, "허용되지 않은 HTTP 메서드입니다."));
  }

  // 매핑된 핸들러가 없거나(NoHandlerFoundException) 정적 리소스가 없을 때(NoResourceFoundException).
  // 예: favicon.ico, firebase-messaging-sw.js 등 브라우저/프론트가 자동 요청하는 경로 → 404로 응답하고
  // ERROR 스택트레이스 대신 WARN 한 줄만 남긴다.
  @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<BaseResponse<Object>> handleNotFound(Exception ex) {
    log.warn("리소스 없음: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(BaseResponse.error(404, "요청한 리소스를 찾을 수 없습니다."));
  }

  @ExceptionHandler(SQLTransientConnectionException.class)
  public ResponseEntity<BaseResponse<Object>> handleConnectionTimeout(
      SQLTransientConnectionException ex) {
    log.error("DB 커넥션 풀 타임아웃: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(BaseResponse.error(503, "서버가 혼잡합니다. 잠시 후 다시 시도해주세요."));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<BaseResponse<Object>> handleAuthentication(AuthenticationException ex) {
    log.warn("인증 실패: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(BaseResponse.error(401, "로그인 인증이 필요합니다."));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<BaseResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
    log.warn("권한 부족: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(BaseResponse.error(403, "접근 권한이 없습니다."));
  }

  @ExceptionHandler(RedisConnectionFailureException.class)
  public ResponseEntity<BaseResponse<Object>> handleRedisConnectionFailure(
      RedisConnectionFailureException ex) {
    log.error("Redis 연결 실패: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(BaseResponse.error(503, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Object>> handleException(Exception ex) {
    log.error("Server 오류 발생: ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(BaseResponse.error(500, "예상치 못한 서버 오류가 발생했습니다."));
  }
}
