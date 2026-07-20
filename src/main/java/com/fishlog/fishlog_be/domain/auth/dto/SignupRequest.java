package com.fishlog.fishlog_be.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 회원가입 요청. 이메일 인증완료 후 호출한다. → docs/security.md §1-3 */
@Schema(description = "회원가입 요청")
public record SignupRequest(
    @Schema(description = "가입 이메일(인증완료된 값)", example = "angler@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
    @Schema(description = "비밀번호(8자 이상, 영문+숫자)", example = "fishlog1234")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.")
        String password,
    @Schema(description = "닉네임(2~10자, 유니크)", example = "붕어킬러")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자입니다.")
        String nickname) {}
