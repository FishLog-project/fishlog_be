package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 이메일 인증코드 발송 응답 — 클라이언트 타이머용 유효시간(초). */
@Schema(description = "이메일 인증코드 발송 응답")
public record EmailSendCodeResponse(
    @Schema(description = "인증코드 유효시간(초)", example = "300") long codeTtlSeconds) {}
