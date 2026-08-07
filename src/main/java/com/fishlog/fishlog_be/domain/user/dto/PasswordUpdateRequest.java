package com.fishlog.fishlog_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 비밀번호 변경 요청(마이페이지, 로그인 상태). 현재 비밀번호 확인 후 새 비밀번호로 교체한다. 비로그인 "비밀번호 찾기"({@code
 * /api/auth/password/reset})와 별개. → docs/security.md
 */
@Schema(description = "비밀번호 변경 요청")
public record PasswordUpdateRequest(
    @Schema(description = "현재 비밀번호", example = "fishlog1234") @NotBlank(message = "현재 비밀번호는 필수입니다.")
        String currentPassword,
    @Schema(description = "새 비밀번호(8자 이상, 영문+숫자)", example = "fishlog5678")
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.")
        String newPassword) {}
