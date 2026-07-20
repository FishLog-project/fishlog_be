package com.fishlog.fishlog_be.domain.collection.entity;

import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 어종 인증 기록(사용자가 잡은 물고기 1건). ERD의 {@code user_dex}를 옵션 B로 구체화한 것.
 *
 * <p><b>옵션 B(인증 1건 = 1행):</b> "감성돔을 3번 잡음"은 catch_count 컬럼이 아니라 이 테이블의 3개 행으로 표현한다. 잡은 횟수 =
 * (userId, fish)로 묶은 행의 개수(COUNT), 획득 여부 = 그 행의 존재 여부. → docs/spec.md
 *
 * <p>{@code userId}는 아직 User 엔티티/인증이 없어 FK 관계 대신 plain Long으로 둔다(임시). JWT 도입 시 로그인 사용자에서 채우고, 필요하면
 * {@code @ManyToOne User}로 승격한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "catch_record")
public class CatchRecord extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 인증한 사용자. User 엔티티/인증 미구현이라 임시로 plain Long(FK 관계 아님). */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 잡은 어종(전체 도감 기준). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fishes_id", nullable = false)
  private Fish fish;

  /** 인증 사진 S3 URL(1건). */
  @Column(name = "certified_image_url", nullable = false)
  private String certifiedImageUrl;

  /**
   * 잡은 어종 크기(cm). 추후 랭킹 산정 기준이라 인증 시 반드시 기록한다(NOT NULL). 이번 조회 응답에는 노출하지 않고 컬럼으로만 적재한다. 동점 처리를 위해
   * 정수가 아닌 {@code Double}로 둔다.
   */
  @Column(nullable = false)
  private Double size;
}
