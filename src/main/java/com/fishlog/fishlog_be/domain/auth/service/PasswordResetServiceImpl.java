package com.fishlog.fishlog_be.domain.auth.service;

import com.fishlog.fishlog_be.domain.auth.exception.AuthErrorCode;
import com.fishlog.fishlog_be.domain.auth.mail.EmailSender;
import com.fishlog.fishlog_be.domain.user.entity.User;
import com.fishlog.fishlog_be.domain.user.repository.UserRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.exception.TooManyRequestsException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link PasswordResetService} 구현. 이메일 인증코드 흐름({@code EmailVerificationServiceImpl})과 동일한 패턴을
 * {@code auth:password:*} 네임스페이스로 복제한다. → docs/security.md
 */
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final String CODE_KEY = "auth:password:code:";
  private static final String RESEND_KEY = "auth:password:resend:";
  private static final String SEND_COUNT_KEY = "auth:password:sendcount:";
  private static final String ATTEMPTS_KEY = "auth:password:attempts:";
  private static final String VERIFIED_KEY = "auth:password:verified:";
  // AuthServiceImpl.REFRESH_KEY와 동일 값(재설정 시 세션 무효화용).
  private static final String REFRESH_KEY = "auth:refresh:";
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StringRedisTemplate redis;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
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

  @Override
  public long sendCode(String email) {
    if (!userRepository.existsByUsername(email)) {
      throw new CustomException(AuthErrorCode.EMAIL_NOT_FOUND);
    }
    enforceResendCooldown(email);
    enforceHourlyLimit(email);

    String code = generateCode();
    redis.opsForValue().set(CODE_KEY + email, code, Duration.ofSeconds(codeTtlSeconds));
    redis.opsForValue().set(RESEND_KEY + email, "1", Duration.ofSeconds(resendCooldownSeconds));
    redis.delete(ATTEMPTS_KEY + email);

    emailSender.sendPasswordResetCode(email, code, codeTtlSeconds);
    return codeTtlSeconds;
  }

  @Override
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

  @Override
  @Transactional
  public void resetPassword(String email, String newPassword) {
    if (!Boolean.TRUE.toString().equals(redis.opsForValue().get(VERIFIED_KEY + email))) {
      throw new CustomException(AuthErrorCode.PASSWORD_RESET_NOT_VERIFIED);
    }
    User user =
        userRepository
            .findByUsername(email)
            .orElseThrow(() -> new CustomException(AuthErrorCode.EMAIL_NOT_FOUND));

    user.changePassword(passwordEncoder.encode(newPassword));

    redis.delete(VERIFIED_KEY + email); // 인증완료 플래그 소비
    redis.delete(REFRESH_KEY + user.getId()); // 기존 세션(refresh) 무효화
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
