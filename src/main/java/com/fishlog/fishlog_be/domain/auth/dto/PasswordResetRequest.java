package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 비밀번호 재설정 요청. 인증코드 확인(verify-code) 완료 후 호출한다. → docs/security.md */
@Schema(description = "비밀번호 재설정 요청")
public record PasswordResetRequest(
    @Schema(description = "재설정할 계정 이메일(인증 완료된 값)", example = "angler@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
    @Schema(description = "새 비밀번호(8자 이상, 영문+숫자)", example = "fishlog5678")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.")
        String newPassword) {}
