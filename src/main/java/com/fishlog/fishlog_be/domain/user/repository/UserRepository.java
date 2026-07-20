package com.fishlog.fishlog_be.domain.user.repository;

import com.fishlog.fishlog_be.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  /** 로그인 이메일로 조회(인증). */
  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  boolean existsByNickname(String nickname);
}
