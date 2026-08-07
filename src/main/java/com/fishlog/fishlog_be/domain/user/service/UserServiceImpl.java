package com.fishlog.fishlog_be.domain.user.service;

import com.fishlog.fishlog_be.domain.user.dto.MyProfileResponse;
import com.fishlog.fishlog_be.domain.user.entity.User;
import com.fishlog.fishlog_be.domain.user.exception.UserErrorCode;
import com.fishlog.fishlog_be.domain.user.repository.UserRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@link UserService} 구현. → docs/security.md */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  // AuthServiceImpl.REFRESH_KEY와 동일 값(비밀번호 변경 시 세션 무효화용).
  private static final String REFRESH_KEY = "auth:refresh:";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate redis;

  @Override
  @Transactional(readOnly = true)
  public MyProfileResponse getMyProfile(Long userId) {
    User user = findUser(userId);
    return new MyProfileResponse(user.getId(), user.getUsername(), user.getNickname());
  }

  @Override
  @Transactional
  public void changeNickname(Long userId, String nickname) {
    User user = findUser(userId);
    if (user.getNickname().equals(nickname)) {
      return; // 현재 닉네임과 동일 → 변경 없음
    }
    if (userRepository.existsByNickname(nickname)) {
      throw new CustomException(UserErrorCode.NICKNAME_ALREADY_EXISTS);
    }
    user.changeNickname(nickname);
  }

  @Override
  @Transactional
  public void changePassword(Long userId, String currentPassword, String newPassword) {
    User user = findUser(userId);
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      throw new CustomException(UserErrorCode.INVALID_CURRENT_PASSWORD);
    }
    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      throw new CustomException(UserErrorCode.SAME_AS_CURRENT_PASSWORD);
    }
    user.changePassword(passwordEncoder.encode(newPassword));
    redis.delete(REFRESH_KEY + userId); // 기존 세션(refresh) 무효화 → 재로그인
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
  }
}
