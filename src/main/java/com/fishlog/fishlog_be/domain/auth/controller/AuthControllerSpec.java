package com.fishlog.fishlog_be.domain.auth.controller;

import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailSendCodeResponse;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeRequest;
import com.fishlog.fishlog_be.domain.auth.dto.EmailVerifyCodeResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
}
