package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Access 재발급 요청. → docs/security.md §2-2 */
@Schema(description = "토큰 재발급 요청")
public record RefreshRequest(
    @Schema(description = "Refresh 토큰", example = "eyJhbGciOi...")
        @NotBlank(message = "refresh 토큰은 필수입니다.")
        String refreshToken) {}
