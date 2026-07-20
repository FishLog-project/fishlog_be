package com.fishlog.fishlog_be.domain.auth.service;

/** 이메일 인증코드 발송·확인. 상태는 Redis에 저장한다(코드·재전송쿨다운·시간당카운트·시도횟수·인증완료 플래그). → docs/security.md §1-1·§1-2 */
public interface EmailVerificationService {

  /** 인증코드 발송. 코드 유효시간(초) 반환. */
  long sendCode(String email);

  /** 인증코드 확인. 성공 시 인증완료 플래그 설정, 유지시간(초) 반환. */
  long verifyCode(String email, String code);

  /** 이메일 인증완료 플래그가 유효한지. (회원가입 단계에서 확인) */
  boolean isVerified(String email);

  /** 인증완료 플래그 소비(가입 완료 시 삭제). */
  void consumeVerified(String email);
}
