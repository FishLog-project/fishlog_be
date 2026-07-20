package com.fishlog.fishlog_be.domain.auth.controller;

import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeResponse;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeResponse;
import com.fishlog.fishlog_be.domain.auth.dto.LoginRequest;
import com.fishlog.fishlog_be.domain.auth.dto.RefreshRequest;
import com.fishlog.fishlog_be.domain.auth.dto.SignupRequest;
import com.fishlog.fishlog_be.domain.auth.dto.TokenResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 인증 API Swagger 문서(문서 전용). → docs/architecture.md, docs/security.md */
@Tag(name = "Auth API", description = "인증(회원가입/로그인) API")
public interface AuthControllerSpec {

  @Operation(
      summary = "이메일 인증코드 발송",
      description =
          """
          ### 설명
          - 미가입 이메일로 6자리 인증코드를 발송합니다. 응답의 codeTtlSeconds로 클라이언트가 타이머를 표시합니다.

          ### 제약조건
          - 허용된 이메일 도메인만 가능(서버 설정에 따름).
          - 재전송 쿨다운 30초, 시간당 발송 5회 제한.

          ### ⚠ 예외상황
          - `EMAIL_ALREADY_EXISTS(409)`: 이미 가입된 이메일
          - `EMAIL_DOMAIN_NOT_ALLOWED(400)`: 허용되지 않은 이메일 도메인
          - 재전송 쿨다운/시간당 한도 초과 시 `429`(data.retryAfterSec 포함)
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "발송 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "인증코드를 발송했습니다.",
                              "data": { "codeTtlSeconds": 300 }
                            }
                            """))),
    @ApiResponse(
        responseCode = "409",
        description = "이미 가입된 이메일",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 409, "message": "이미 가입된 이메일입니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "429",
        description = "요청 빈도 제한(재전송 쿨다운/시간당 한도)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 429, "message": "잠시 후 다시 요청해주세요.", "data": { "retryAfterSec": 30 } }
                            """)))
  })
  BaseResponse<EmailSendCodeResponse> sendCode(EmailSendCodeRequest request);

  @Operation(
      summary = "이메일 인증코드 확인",
      description =
          """
          ### 설명
          - 발송된 6자리 코드를 확인합니다. 일치 시 인증완료 상태를 부여(verifiedTtlSeconds 동안 유지)해 회원가입 단계로 진행할 수 있습니다.

          ### 제약조건
          - 코드는 6자리 숫자.
          - 5회 연속 오입력 시 코드가 무효화되어 재발송이 필요합니다.

          ### ⚠ 예외상황
          - `VERIFICATION_CODE_EXPIRED(400)`: 코드가 만료되었거나 발급되지 않음
          - `VERIFICATION_CODE_MISMATCH(400)`: 코드 불일치
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "인증 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "이메일 인증이 완료되었습니다.",
                              "data": { "verifiedTtlSeconds": 600 }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "코드 만료/불일치",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "인증코드가 일치하지 않습니다.", "data": null }
                            """)))
  })
  BaseResponse<EmailVerifyCodeResponse> verifyCode(EmailVerifyCodeRequest request);

  @Operation(
      summary = "회원가입",
      description =
          """
          ### 설명
          - 이메일 인증완료 상태에서 비밀번호·닉네임으로 가입합니다. 성공 시 즉시 로그인되어 토큰(Access/Refresh)을 발급합니다.

          ### 제약조건
          - 비밀번호: 8자 이상, 영문+숫자 포함.
          - 닉네임: 2~10자, 유니크.

          ### ⚠ 예외상황
          - `EMAIL_NOT_VERIFIED(400)`: 이메일 인증이 완료되지 않음
          - `EMAIL_ALREADY_EXISTS(409)`: 이미 가입된 이메일
          - `NICKNAME_ALREADY_EXISTS(409)`: 이미 사용 중인 닉네임
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "가입 성공(토큰 발급)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "회원가입이 완료되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "붕어킬러",
                                "accessToken": "eyJhbGciOi...",
                                "refreshToken": "eyJhbGciOi...",
                                "accessTokenExpiresIn": 1800
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "409",
        description = "이메일/닉네임 중복",
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
  BaseResponse<TokenResponse> signup(SignupRequest request);

  @Operation(
      summary = "로그인",
      description =
          """
          ### 설명
          - 이메일/비밀번호로 로그인해 Access/Refresh 토큰을 발급합니다.

          ### ⚠ 예외상황
          - `INVALID_CREDENTIALS(401)`: 이메일 미존재 또는 비밀번호 불일치(계정 열거 방지를 위해 동일 메시지)
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "로그인 성공(토큰 발급)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "로그인되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "붕어킬러",
                                "accessToken": "eyJhbGciOi...",
                                "refreshToken": "eyJhbGciOi...",
                                "accessTokenExpiresIn": 1800
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 401, "message": "이메일 또는 비밀번호가 올바르지 않습니다.", "data": null }
                            """)))
  })
  BaseResponse<TokenResponse> login(LoginRequest request);

  @Operation(
      summary = "토큰 재발급(회전)",
      description =
          """
          ### 설명
          - Refresh 토큰으로 새 Access/Refresh를 발급합니다(회전 — 기존 refresh는 무효화).

          ### ⚠ 예외상황
          - `INVALID_REFRESH_TOKEN(401)`: 서명/만료 무효, 또는 서버 저장값과 불일치(재사용·탈취 의심 시 무효화)
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "재발급 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "토큰이 재발급되었습니다.",
                              "data": {
                                "userId": 1,
                                "nickname": "붕어킬러",
                                "accessToken": "eyJhbGciOi...",
                                "refreshToken": "eyJhbGciOi...",
                                "accessTokenExpiresIn": 1800
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "유효하지 않은 refresh 토큰",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 401, "message": "유효하지 않은 refresh 토큰입니다.", "data": null }
                            """)))
  })
  BaseResponse<TokenResponse> refresh(RefreshRequest request);

  @Operation(
      summary = "로그아웃",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 서버에 저장된 Refresh 토큰을 삭제해 재발급을 차단합니다. Access는 만료까지 유효합니다.
          - `Authorization: Bearer {accessToken}` 필요.

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "로그아웃 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": true, "code": 200, "message": "로그아웃되었습니다.", "data": null }
                            """)))
  })
  BaseResponse<Void> logout(@Parameter(hidden = true) Long userId);
}
