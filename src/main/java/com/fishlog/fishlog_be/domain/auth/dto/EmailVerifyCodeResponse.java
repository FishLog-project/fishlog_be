package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 이메일 인증코드 확인 응답 — 인증완료 상태가 유지되는(가입 제한) 시간(초). */
@Schema(description = "이메일 인증코드 확인 응답")
public record EmailVerifyCodeResponse(
    @Schema(description = "가입 완료까지 인증상태 유지시간(초)", example = "600") long verifiedTtlSeconds) {}
