package com.fishlog.fishlog_be.domain.auth.service;

import com.fishlog.fishlog_be.domain.auth.dto.SignupRequest;
import com.fishlog.fishlog_be.domain.auth.dto.SignupResponse;
import com.fishlog.fishlog_be.domain.auth.dto.TokenResponse;

/** 회원가입·로그인 등 인증 흐름. 토큰 발급/저장을 담당한다. → docs/security.md §1-3·§2 */
public interface AuthService {

  /** 회원가입. 이메일 인증완료 상태여야 한다. 토큰은 발급하지 않으며, 가입 후 로그인 API로 발급받는다. */
  SignupResponse signup(SignupRequest request);

  /** 로그인. 이메일/비밀번호 검증 후 토큰 발급. 실패는 계정 열거 방지를 위해 동일 메시지. */
  TokenResponse login(String email, String rawPassword);

  /** Refresh 회전. 서명·타입·Redis 저장값 일치 검증 후 새 Access/Refresh 발급(구 refresh 무효화). */
  TokenResponse refresh(String refreshToken);

  /** 로그아웃. 서버의 refresh 삭제(재발급 차단). 인증되지 않은 호출은 401. */
  void logout(Long userId);
}
