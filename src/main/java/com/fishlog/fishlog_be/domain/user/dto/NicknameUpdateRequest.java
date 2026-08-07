package com.fishlog.fishlog_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 닉네임 변경 요청(마이페이지). 회원가입과 동일한 닉네임 규칙(2~10자). */
@Schema(description = "닉네임 변경 요청")
public record NicknameUpdateRequest(
    @Schema(description = "새 닉네임(2~10자, 유니크)", example = "감성돔사냥꾼")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자입니다.")
        String nickname) {}
