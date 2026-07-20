package com.fishlog.fishlog_be.domain.auth.controller;

import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeResponse;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeResponse;
import com.fishlog.fishlog_be.domain.auth.service.EmailVerificationService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증(회원가입/로그인) API. → docs/security.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

  private final EmailVerificationService emailVerificationService;

  @PostMapping("/email/send-code")
  @Operation(summary = "이메일 인증코드 발송", description = "미가입 이메일로 6자리 코드를 발송한다(재전송 쿨다운·시간당 한도 적용).")
  public BaseResponse<EmailSendCodeResponse> sendCode(
      @Valid @RequestBody EmailSendCodeRequest request) {
    long ttl = emailVerificationService.sendCode(request.email());
    return BaseResponse.success("인증코드를 발송했습니다.", new EmailSendCodeResponse(ttl));
  }

  @PostMapping("/email/verify-code")
  @Operation(summary = "이메일 인증코드 확인", description = "코드 일치 시 인증완료 상태를 부여한다(가입 단계로 진행 가능).")
  public BaseResponse<EmailVerifyCodeResponse> verifyCode(
      @Valid @RequestBody EmailVerifyCodeRequest request) {
    long ttl = emailVerificationService.verifyCode(request.email(), request.code());
    return BaseResponse.success("이메일 인증이 완료되었습니다.", new EmailVerifyCodeResponse(ttl));
  }
}
