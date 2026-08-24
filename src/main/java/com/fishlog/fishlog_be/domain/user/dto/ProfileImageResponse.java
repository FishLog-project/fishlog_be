package com.fishlog.fishlog_be.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** 프로필 이미지 업로드 응답. */
@Schema(description = "프로필 이미지 업로드 결과")
public record ProfileImageResponse(
    @Schema(
            description = "업로드된 프로필 이미지 URL",
            example = "https://fishlog-bucket.s3.ap-northeast-2.amazonaws.com/profile/uuid.png")
        String profileImageUrl) {}
