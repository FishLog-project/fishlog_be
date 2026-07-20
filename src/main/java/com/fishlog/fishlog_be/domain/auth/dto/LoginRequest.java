package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** 로그인 요청. → docs/security.md §2 */
@Schema(description = "로그인 요청")
public record LoginRequest(
    @Schema(description = "이메일", example = "angler@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
    @Schema(description = "비밀번호", example = "fishlog1234") @NotBlank(message = "비밀번호는 필수입니다.")
        String password) {}
