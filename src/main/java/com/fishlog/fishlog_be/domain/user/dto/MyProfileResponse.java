package com.fishlog.fishlog_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 마이페이지 내 프로필 응답. */
@Schema(description = "내 프로필")
public record MyProfileResponse(
    @Schema(description = "사용자 ID", example = "1") Long userId,
    @Schema(description = "로그인 이메일", example = "angler@gmail.com") String email,
    @Schema(description = "닉네임", example = "붕어킬러") String nickname,
    @Schema(
            description = "프로필 이미지 URL(미설정 시 null)",
            example = "https://fishlog-bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid.png")
        String profileImageUrl) {}
