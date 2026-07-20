package com.fishlog.fishlog_be.domain.auth.service;

import com.fishlog.fishlog_be.domain.auth.dto.SignupRequest;
import com.fishlog.fishlog_be.domain.auth.dto.TokenResponse;
import com.fishlog.fishlog_be.domain.auth.exception.AuthErrorCode;
import com.fishlog.fishlog_be.domain.user.entity.User;
import com.fishlog.fishlog_be.domain.user.repository.UserRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.jwt.JwtProvider;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원가입·로그인 등 인증 흐름. 토큰 발급/저장을 담당한다. → docs/security.md §1-3·§2 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String REFRESH_KEY = "auth:refresh:";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;
  private final EmailVerificationService emailVerificationService;
  private final StringRedisTemplate redis;

  /** 회원가입 → 즉시 로그인(토큰 발급). 이메일 인증완료 상태여야 한다. */
  @Transactional
  public TokenResponse signup(SignupRequest request) {
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
    return issueTokens(user);
  }

  /** Access/Refresh 발급 + Refresh를 Redis에 저장(사용자당 1개). 로그인(#31)에서도 재사용. */
  public TokenResponse issueTokens(User user) {
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
