package com.fishlog.fishlog_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 회원탈퇴 요청. 본인 확인을 위해 현재 비밀번호를 받는다. → docs/security.md */
@Schema(description = "회원탈퇴 요청")
public record WithdrawRequest(
    @Schema(description = "현재 비밀번호(본인 확인)", example = "fishlog1234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password) {}
