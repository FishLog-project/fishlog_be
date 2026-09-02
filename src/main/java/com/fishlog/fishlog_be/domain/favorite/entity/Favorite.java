package com.fishlog.fishlog_be.domain.favorite.entity;

import com.fishlog.fishlog_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 스팟 찜(즐겨찾기) 기록 — 사용자↔스팟 N:M. `catch_record` 패턴을 따르되 "1회 찜 = 1행, 중복 불가"라 (user_id, spot_id) UNIQUE.
 *
 * <p>{@code userId}·{@code spotId}는 조회 시 토큰/경로에서 채우며, 컬럼은 아직 FK가 아닌 plain Long이다(승격은 별도 작업 →
 * docs/auth-followup.md). {@code createdAt}(BaseTimeEntity)이 찜한 시각. → docs/spec.md
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "favorite",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_favorite_user_spot",
            columnNames = {"user_id", "spot_id"}))
public class Favorite extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 찜한 사용자 id. {@code users.id}를 가리키지만 FK 관계는 아직 아니다(plain Long). */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 찜한 스팟 id. {@code spots.id}를 가리키지만 FK 관계는 아직 아니다(plain Long). */
  @Column(name = "spot_id", nullable = false)
  private Long spotId;
}
