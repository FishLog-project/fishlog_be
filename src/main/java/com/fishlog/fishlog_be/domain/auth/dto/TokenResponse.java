package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 인증 토큰 응답(회원가입·로그인 공통). → docs/spec.md, docs/security.md §2 */
@Schema(description = "인증 토큰 응답")
public record TokenResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "닉네임", example = "붕어킬러") String nickname,
    @Schema(description = "Access 토큰", example = "eyJhbGciOi...") String accessToken,
    @Schema(description = "Refresh 토큰", example = "eyJhbGciOi...") String refreshToken,
    @Schema(description = "Access 토큰 만료(초)", example = "1800") long accessTokenExpiresIn) {}
