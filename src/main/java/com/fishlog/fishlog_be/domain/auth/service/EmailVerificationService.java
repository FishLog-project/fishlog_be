package com.fishlog.fishlog_be.domain.auth.service;

import com.fishlog.fishlog_be.domain.auth.exception.AuthErrorCode;
import com.fishlog.fishlog_be.domain.auth.mail.EmailSender;
import com.fishlog.fishlog_be.domain.user.repository.UserRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.exception.TooManyRequestsException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 이메일 인증코드 발송·확인. 상태는 Redis에 저장한다(코드·재전송쿨다운·시간당카운트·시도횟수·인증완료 플래그). → docs/security.md §1-1·§1-2 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private static final String CODE_KEY = "auth:email:code:";
  private static final String RESEND_KEY = "auth:email:resend:";
  private static final String SEND_COUNT_KEY = "auth:email:sendcount:";
  private static final String ATTEMPTS_KEY = "auth:email:attempts:";
  private static final String VERIFIED_KEY = "auth:email:verified:";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StringRedisTemplate redis;
  private final UserRepository userRepository;
  private final EmailSender emailSender;

  @Value("${auth.email.code-ttl-seconds:300}")
  private long codeTtlSeconds;

  @Value("${auth.email.resend-cooldown-seconds:30}")
  private long resendCooldownSeconds;

  @Value("${auth.email.hourly-send-limit:5}")
  private long hourlySendLimit;

  @Value("${auth.email.max-verify-attempts:5}")
  private long maxVerifyAttempts;

  @Value("${auth.email.verified-ttl-seconds:600}")
  private long verifiedTtlSeconds;

  @Value("${auth.allowed-email-domains:}")
  private String allowedEmailDomains;

  /** 인증코드 발송. 코드 유효시간(초) 반환. */
  public long sendCode(String email) {
    validateDomain(email);
    if (userRepository.existsByUsername(email)) {
      throw new CustomException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
    enforceResendCooldown(email);
    enforceHourlyLimit(email);

    String code = generateCode();
    redis.opsForValue().set(CODE_KEY + email, code, Duration.ofSeconds(codeTtlSeconds));
    redis.opsForValue().set(RESEND_KEY + email, "1", Duration.ofSeconds(resendCooldownSeconds));
    redis.delete(ATTEMPTS_KEY + email);

    emailSender.sendVerificationCode(email, code, codeTtlSeconds);
    return codeTtlSeconds;
  }

  /** 인증코드 확인. 성공 시 인증완료 플래그 설정, 유지시간(초) 반환. */
  public long verifyCode(String email, String code) {
    String stored = redis.opsForValue().get(CODE_KEY + email);
    if (stored == null) {
      throw new CustomException(AuthErrorCode.VERIFICATION_CODE_EXPIRED);
    }
    if (!stored.equals(code)) {
      handleMismatch(email);
      throw new CustomException(AuthErrorCode.VERIFICATION_CODE_MISMATCH);
    }
    redis.opsForValue().set(VERIFIED_KEY + email, "true", Duration.ofSeconds(verifiedTtlSeconds));
    redis.delete(Arrays.asList(CODE_KEY + email, ATTEMPTS_KEY + email));
    return verifiedTtlSeconds;
  }

  /** 이메일 인증완료 플래그가 유효한지. (회원가입 단계에서 확인) */
  public boolean isVerified(String email) {
    return Boolean.TRUE.toString().equals(redis.opsForValue().get(VERIFIED_KEY + email));
  }

  /** 인증완료 플래그 소비(가입 완료 시 삭제). */
  public void consumeVerified(String email) {
    redis.delete(VERIFIED_KEY + email);
  }

  private void validateDomain(String email) {
    Set<String> allowed =
        Arrays.stream(allowedEmailDomains.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(s -> s.toLowerCase())
            .collect(Collectors.toSet());
    if (allowed.isEmpty()) {
      return; // 제한 없음
    }
    int at = email.lastIndexOf('@');
    String domain = at >= 0 ? email.substring(at + 1).toLowerCase() : "";
    if (!allowed.contains(domain)) {
      throw new CustomException(AuthErrorCode.EMAIL_DOMAIN_NOT_ALLOWED);
    }
  }

  private void enforceResendCooldown(String email) {
    Long ttl = redis.getExpire(RESEND_KEY + email, TimeUnit.SECONDS);
    if (ttl != null && ttl > 0) {
      throw new TooManyRequestsException("잠시 후 다시 요청해주세요.", ttl.intValue());
    }
  }

  private void enforceHourlyLimit(String email) {
    String key = SEND_COUNT_KEY + email;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, Duration.ofHours(1));
    }
    if (count != null && count > hourlySendLimit) {
      Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
      throw new TooManyRequestsException("시간당 발송 한도를 초과했습니다.", ttl != null ? ttl.intValue() : 3600);
    }
  }

  private void handleMismatch(String email) {
    String key = ATTEMPTS_KEY + email;
    Long attempts = redis.opsForValue().increment(key);
    if (attempts != null && attempts == 1L) {
      redis.expire(key, Duration.ofSeconds(codeTtlSeconds));
    }
    if (attempts != null && attempts >= maxVerifyAttempts) {
      // brute-force 방지: 코드 무효화(재발송 필요)
      redis.delete(Arrays.asList(CODE_KEY + email, key));
    }
  }

  private String generateCode() {
    return String.format("%06d", RANDOM.nextInt(1_000_000));
  }
}
