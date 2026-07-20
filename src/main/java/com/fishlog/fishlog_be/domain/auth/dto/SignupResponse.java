package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 회원가입 응답. 토큰은 발급하지 않는다(로그인 API에서 발급). → docs/security.md §1-3 */
@Schema(description = "회원가입 응답")
public record SignupResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "닉네임", example = "붕어킬러") String nickname) {}
