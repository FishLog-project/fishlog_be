package com.fishlog.fishlog_be.domain.user.entity;

import com.fishlog.fishlog_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 사용자 — 자체 이메일/비밀번호 로그인 주체. ERD 기준. → docs/spec.md "users", docs/security.md
 *
 * <p>비밀번호는 항상 BCrypt 해시로만 저장한다(평문 금지). 이메일 인증코드·refresh 토큰은 DB가 아닌 Redis에 저장한다. 권한(role)은 추후 구현 예정.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "users")
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 로그인 이메일. */
  @Column(nullable = false, unique = true)
  private String username;

  /** BCrypt 해시. */
  @Column(name = "password_hash", nullable = false)
  private String password;

  /** 표시 이름(2~10자, 유니크). */
  @Column(nullable = false, unique = true)
  private String nickname;
}
