package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 이메일 인증코드 확인 요청. */
@Schema(description = "이메일 인증코드 확인 요청")
public record EmailVerifyCodeRequest(
    @Schema(description = "인증 이메일", example = "angler@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
    @Schema(description = "6자리 인증코드", example = "042715")
        @NotBlank(message = "인증코드는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "인증코드는 6자리 숫자입니다.")
        String code) {}
