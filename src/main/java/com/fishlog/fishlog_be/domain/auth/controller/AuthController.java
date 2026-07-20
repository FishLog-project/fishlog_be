package com.fishlog.fishlog_be.domain.auth.controller;

import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeResponse;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeResponse;
import com.fishlog.fishlog_be.domain.auth.dto.LoginRequest;
import com.fishlog.fishlog_be.domain.auth.dto.RefreshRequest;
import com.fishlog.fishlog_be.domain.auth.dto.SignupRequest;
import com.fishlog.fishlog_be.domain.auth.dto.SignupResponse;
import com.fishlog.fishlog_be.domain.auth.dto.TokenResponse;
import com.fishlog.fishlog_be.domain.auth.service.AuthService;
import com.fishlog.fishlog_be.domain.auth.service.EmailVerificationService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증(회원가입/로그인) API. 문서는 {@link AuthControllerSpec}. → docs/security.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerSpec {

  private final EmailVerificationService emailVerificationService;
  private final AuthService authService;

  @Override
  @PostMapping("/email/send-code")
  public BaseResponse<EmailSendCodeResponse> sendCode(
      @Valid @RequestBody EmailSendCodeRequest request) {
    long ttl = emailVerificationService.sendCode(request.email());
    return BaseResponse.success("인증코드를 발송했습니다.", new EmailSendCodeResponse(ttl));
  }

  @Override
  @PostMapping("/email/verify-code")
  public BaseResponse<EmailVerifyCodeResponse> verifyCode(
      @Valid @RequestBody EmailVerifyCodeRequest request) {
    long ttl = emailVerificationService.verifyCode(request.email(), request.code());
    return BaseResponse.success("이메일 인증이 완료되었습니다.", new EmailVerifyCodeResponse(ttl));
  }

  @Override
  @PostMapping("/signup")
  public BaseResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return BaseResponse.success("회원가입이 완료되었습니다.", authService.signup(request));
  }

  @Override
  @PostMapping("/login")
  public BaseResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return BaseResponse.success(
        "로그인되었습니다.", authService.login(request.email(), request.password()));
  }

  @Override
  @PostMapping("/refresh")
  public BaseResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return BaseResponse.success("토큰이 재발급되었습니다.", authService.refresh(request.refreshToken()));
  }

  @Override
  @PostMapping("/logout")
  public BaseResponse<Void> logout(@AuthenticationPrincipal Long userId) {
    authService.logout(userId);
    return BaseResponse.success("로그아웃되었습니다.", null);
  }
}
