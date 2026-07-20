package com.fishlog.fishlog_be.domain.auth.service;

import com.fishlog.fishlog_be.domain.auth.dto.SignupRequest;
import com.fishlog.fishlog_be.domain.auth.dto.SignupResponse;
import com.fishlog.fishlog_be.domain.auth.dto.TokenResponse;
import com.fishlog.fishlog_be.domain.auth.exception.AuthErrorCode;
import com.fishlog.fishlog_be.domain.user.entity.User;
import com.fishlog.fishlog_be.domain.user.repository.UserRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.exception.GlobalErrorCode;
import com.fishlog.fishlog_be.global.jwt.JwtProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link AuthService} 구현. 가입·로그인·재발급·로그아웃 및 토큰 발급/저장. → docs/security.md §1-3·§2 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final String REFRESH_KEY = "auth:refresh:";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;
  private final EmailVerificationService emailVerificationService;
  private final StringRedisTemplate redis;

  @Override
  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (!emailVerificationService.isVerified(request.email())) {
      throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
    }
    if (userRepository.existsByUsername(request.email())) {
      throw new CustomException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
    if (userRepository.existsByNickname(request.nickname())) {
      throw new CustomException(AuthErrorCode.NICKNAME_ALREADY_EXISTS);
    }

    User user =
        userRepository.save(
            User.builder()
                .username(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build());

    emailVerificationService.consumeVerified(request.email());
    return new SignupResponse(user.getId(), user.getNickname());
  }

  @Override
  @Transactional(readOnly = true)
  public TokenResponse login(String email, String rawPassword) {
    User user =
        userRepository
            .findByUsername(email)
            .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_CREDENTIALS));
    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
      throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
    }
    return issueTokens(user);
  }

  @Override
  @Transactional(readOnly = true)
  public TokenResponse refresh(String refreshToken) {
    if (!jwtProvider.validate(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
      throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    Long userId = jwtProvider.getUserId(refreshToken);
    String stored = redis.opsForValue().get(REFRESH_KEY + userId);
    if (stored == null || !stored.equals(refreshToken)) {
      // 저장값과 불일치 = 재사용/탈취 의심 → 무효화하고 거부.
      redis.delete(REFRESH_KEY + userId);
      throw new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_REFRESH_TOKEN));
    return issueTokens(user); // Redis refresh 교체(회전)
  }

  @Override
  public void logout(Long userId) {
    if (userId == null) {
      throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
    }
    redis.delete(REFRESH_KEY + userId);
  }

  /** Access/Refresh 발급 + Refresh를 Redis에 저장(사용자당 1개). */
  private TokenResponse issueTokens(User user) {
    String accessToken = jwtProvider.createAccessToken(user.getId());
    String refreshToken = jwtProvider.createRefreshToken(user.getId());
    redis
        .opsForValue()
        .set(
            REFRESH_KEY + user.getId(),
            refreshToken,
            Duration.ofSeconds(jwtProvider.getRefreshTtlSeconds()));
    return new TokenResponse(
        user.getId(),
        user.getNickname(),
        accessToken,
        refreshToken,
        jwtProvider.getAccessTtlSeconds());
  }
}
