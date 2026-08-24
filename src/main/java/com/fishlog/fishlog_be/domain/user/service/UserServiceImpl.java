package com.fishlog.fishlog_be.domain.user.service;

import com.fishlog.fishlog_be.domain.collection.service.CollectionService;
import com.fishlog.fishlog_be.domain.user.dto.MyProfileResponse;
import com.fishlog.fishlog_be.domain.user.entity.User;
import com.fishlog.fishlog_be.domain.user.exception.UserErrorCode;
import com.fishlog.fishlog_be.domain.user.repository.UserRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.s3.PathName;
import com.fishlog.fishlog_be.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** {@link UserService} 구현. → docs/security.md */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

  // AuthServiceImpl.REFRESH_KEY와 동일 값(비밀번호 변경 시 세션 무효화용).
  private static final String REFRESH_KEY = "auth:refresh:";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate redis;
  // 도메인 경계: collection 의 repository·entity 직접 접근 대신 service 인터페이스로만 호출.
  private final CollectionService collectionService;
  private final S3Service s3Service;

  @Override
  @Transactional(readOnly = true)
  public MyProfileResponse getMyProfile(Long userId) {
    User user = findUser(userId);
    return new MyProfileResponse(
        user.getId(), user.getUsername(), user.getNickname(), user.getProfileImageUrl());
  }

  @Override
  @Transactional
  public String updateProfileImage(Long userId, MultipartFile image) {
    User user = findUser(userId);
    String oldUrl = user.getProfileImageUrl();

    String newUrl = s3Service.upload(image, PathName.PROFILE);
    user.changeProfileImage(newUrl);

    // 기존 이미지 정리(best-effort). 실패해도 교체는 성공 처리.
    if (oldUrl != null && !oldUrl.isBlank()) {
      try {
        s3Service.delete(oldUrl);
      } catch (Exception e) {
        log.warn("[profile-image] 이전 이미지 삭제 실패(무시): url={}, {}", oldUrl, e.getMessage());
      }
    }
    return newUrl;
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

  @Override
  @Transactional
  public void withdraw(Long userId, String password) {
    User user = findUser(userId);
    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new CustomException(UserErrorCode.INVALID_CURRENT_PASSWORD);
    }
    collectionService.deleteMyRecords(userId); // 도감 인증기록 삭제(랭킹 유령 방지)
    userRepository.delete(user); // 사용자 하드 삭제
    redis.delete(REFRESH_KEY + userId); // refresh 무효화
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
  }
}
