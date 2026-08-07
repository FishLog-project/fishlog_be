package com.fishlog.fishlog_be.domain.user.service;

import com.fishlog.fishlog_be.domain.user.dto.MyProfileResponse;

/** 마이페이지(내 프로필 조회·닉네임 변경·비밀번호 변경). → docs/security.md */
public interface UserService {

  /** 내 프로필 조회. */
  MyProfileResponse getMyProfile(Long userId);

  /** 닉네임 변경. 유니크 검증 후 교체. 현재 닉네임과 같으면 no-op. */
  void changeNickname(Long userId, String nickname);

  /** 비밀번호 변경. 현재 비밀번호 확인 후 교체하고 기존 refresh를 무효화한다(재로그인). */
  void changePassword(Long userId, String currentPassword, String newPassword);

  /** 회원탈퇴. 현재 비밀번호 확인 후 사용자·도감 인증기록·refresh를 삭제한다(하드 삭제). */
  void withdraw(Long userId, String password);
}
