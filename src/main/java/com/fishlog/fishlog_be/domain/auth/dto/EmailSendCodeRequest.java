package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 이메일 인증코드 발송 요청. */
@Schema(description = "이메일 인증코드 발송 요청")
public record EmailSendCodeRequest(
    @Schema(description = "인증받을 이메일", example = "angler@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email) {}
