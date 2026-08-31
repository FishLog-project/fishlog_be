package com.fishlog.fishlog_be.domain.favorite.controller;

import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 스팟 찜(즐겨찾기) API Swagger 문서(문서 전용). → docs/architecture.md, docs/spec.md */
@Tag(name = "Favorite API", description = "낚시 스팟 찜(즐겨찾기) API")
public interface FavoriteControllerSpec {

  @Operation(
      summary = "스팟 찜 추가",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 해당 스팟을 로그인 사용자의 찜 목록에 추가합니다.
          - **idempotent**: 이미 찜한 스팟이면 중복 없이 그대로 성공합니다.
          - `Authorization: Bearer {accessToken}` 필요.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          - `SPOT_NOT_FOUND(404)`: 해당 id의 스팟이 없는 경우
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "찜 추가 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": true, "code": 200, "message": "스팟을 찜했습니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "스팟을 찾을 수 없음(S001)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 404, "message": "스팟을 찾을 수 없습니다.", "data": null }
                            """)))
  })
  BaseResponse<Void> addFavorite(
      @Parameter(hidden = true) Long userId, @Parameter(example = "1") Long spotId);

  @Operation(
      summary = "스팟 찜 해제",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 해당 스팟을 로그인 사용자의 찜 목록에서 제거합니다.
          - **idempotent**: 찜 상태가 아니어도 그대로 성공합니다.
          - `Authorization: Bearer {accessToken}` 필요.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "찜 해제 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": true, "code": 200, "message": "찜을 해제했습니다.", "data": null }
                            """)))
  })
  BaseResponse<Void> removeFavorite(
      @Parameter(hidden = true) Long userId, @Parameter(example = "1") Long spotId);
}
