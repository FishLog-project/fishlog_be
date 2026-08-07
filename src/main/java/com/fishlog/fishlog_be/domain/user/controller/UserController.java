package com.fishlog.fishlog_be.domain.user.controller;

import com.fishlog.fishlog_be.domain.user.dto.MyProfileResponse;
import com.fishlog.fishlog_be.domain.user.dto.NicknameUpdateRequest;
import com.fishlog.fishlog_be.domain.user.dto.PasswordUpdateRequest;
import com.fishlog.fishlog_be.domain.user.service.UserService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지(User) API. 문서는 {@link UserControllerSpec}. → docs/security.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController implements UserControllerSpec {

  private final UserService userService;

  @Override
  @GetMapping("/me")
  public BaseResponse<MyProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
    return BaseResponse.success(userService.getMyProfile(userId));
  }

  @Override
  @PatchMapping("/me/nickname")
  public BaseResponse<Void> changeNickname(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody NicknameUpdateRequest request) {
    userService.changeNickname(userId, request.nickname());
    return BaseResponse.success("닉네임이 변경되었습니다.", null);
  }

  @Override
  @PatchMapping("/me/password")
  public BaseResponse<Void> changePassword(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody PasswordUpdateRequest request) {
    userService.changePassword(userId, request.currentPassword(), request.newPassword());
    return BaseResponse.success("비밀번호가 변경되었습니다.", null);
  }
}
