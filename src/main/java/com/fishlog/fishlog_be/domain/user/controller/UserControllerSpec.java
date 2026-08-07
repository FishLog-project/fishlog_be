package com.fishlog.fishlog_be.domain.user.controller;

import com.fishlog.fishlog_be.domain.user.dto.MyProfileResponse;
import com.fishlog.fishlog_be.domain.user.dto.NicknameUpdateRequest;
import com.fishlog.fishlog_be.domain.user.dto.PasswordUpdateRequest;
import com.fishlog.fishlog_be.domain.user.dto.WithdrawRequest;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 마이페이지(User) API Swagger 문서(문서 전용). → docs/architecture.md, docs/security.md */
@Tag(name = "User API", description = "마이페이지(내 프로필/닉네임/비밀번호) API")
public interface UserControllerSpec {

  @Operation(
      summary = "내 프로필 조회",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 로그인한 사용자의 프로필(이메일·닉네임)을 반환합니다.
          - `Authorization: Bearer {accessToken}` 필요.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          - `USER_NOT_FOUND(404)`: 토큰의 사용자가 존재하지 않음
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MyProfileResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": { "userId": 1, "email": "angler@gmail.com", "nickname": "붕어킬러" }
                            }
                            """)))
  })
  BaseResponse<MyProfileResponse> getMyProfile(@Parameter(hidden = true) Long userId);

  @Operation(
      summary = "닉네임 변경",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 로그인한 사용자의 닉네임을 변경합니다.
          - 현재 닉네임과 동일한 값을 보내면 변경 없이 성공 처리됩니다.
          - `Authorization: Bearer {accessToken}` 필요.

          ### 제약조건
          - 닉네임 2~10자, 유니크.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          - `USER_NOT_FOUND(404)`: 토큰의 사용자가 존재하지 않음
          - `NICKNAME_ALREADY_EXISTS(409)`: 이미 사용 중인 닉네임
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "변경 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": true, "code": 200, "message": "닉네임이 변경되었습니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "409",
        description = "닉네임 중복",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 409, "message": "이미 사용 중인 닉네임입니다.", "data": null }
                            """)))
  })
  BaseResponse<Void> changeNickname(
      @Parameter(hidden = true) Long userId, NicknameUpdateRequest request);

  @Operation(
      summary = "비밀번호 변경",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 로그인한 사용자가 **현재 비밀번호 확인 후** 새 비밀번호로 변경합니다.
          - 비로그인 "비밀번호 찾기"(`/api/auth/password/reset`)와 별개입니다.
          - 변경 성공 시 기존 refresh 토큰이 무효화되어 **재로그인**이 필요합니다.
          - `Authorization: Bearer {accessToken}` 필요.

          ### 제약조건
          - 새 비밀번호 8자 이상, 영문+숫자 포함.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          - `USER_NOT_FOUND(404)`: 토큰의 사용자가 존재하지 않음
          - `INVALID_CURRENT_PASSWORD(400)`: 현재 비밀번호 불일치
          - `SAME_AS_CURRENT_PASSWORD(400)`: 새 비밀번호가 현재와 동일
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "변경 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": true, "code": 200, "message": "비밀번호가 변경되었습니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "현재 비밀번호 불일치 등",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "현재 비밀번호가 올바르지 않습니다.", "data": null }
                            """)))
  })
  BaseResponse<Void> changePassword(
      @Parameter(hidden = true) Long userId, PasswordUpdateRequest request);

  @Operation(
      summary = "회원탈퇴",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 로그인한 사용자가 **현재 비밀번호 확인 후** 계정을 삭제(하드 삭제)합니다.
          - 사용자의 **도감 인증기록도 함께 삭제**되며, 기존 refresh 토큰이 무효화됩니다(되돌릴 수 없음).
          - `Authorization: Bearer {accessToken}` 필요.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          - `USER_NOT_FOUND(404)`: 토큰의 사용자가 존재하지 않음
          - `INVALID_CURRENT_PASSWORD(400)`: 비밀번호 불일치
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "탈퇴 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": true, "code": 200, "message": "회원탈퇴가 완료되었습니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "비밀번호 불일치",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "현재 비밀번호가 올바르지 않습니다.", "data": null }
                            """)))
  })
  BaseResponse<Void> withdraw(@Parameter(hidden = true) Long userId, WithdrawRequest request);
}
