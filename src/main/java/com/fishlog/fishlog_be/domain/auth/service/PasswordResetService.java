package com.fishlog.fishlog_be.domain.auth.service;

/**
 * 비밀번호 찾기(재설정). 이메일 인증코드 흐름을 미러링하되, 대상은 **가입된 사용자**이고 최종 단계에서 비밀번호를 교체한다. 상태는 Redis {@code
 * auth:password:*} 네임스페이스에 저장한다. → docs/security.md
 */
public interface PasswordResetService {

  /** 재설정 인증코드 발송. 가입된 이메일이어야 한다. 코드 유효시간(초) 반환. */
  long sendCode(String email);

  /** 인증코드 확인. 성공 시 재설정 인증완료 플래그 설정, 유지시간(초) 반환. */
  long verifyCode(String email, String code);

  /** 비밀번호 재설정. 인증완료 상태여야 한다. 성공 시 기존 refresh 토큰을 무효화한다(토큰 미발급). */
  void resetPassword(String email, String newPassword);
}
