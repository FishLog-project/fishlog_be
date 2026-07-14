package com.fishlog.fishlog_be.domain.fish.entity;

import com.fishlog.fishlog_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 어종 — 도감 기준 데이터. ERD v0.1 기준.
 *
 * <p>시드 단계에서는 {@code name}만 채워지며(바다낚시지수 API 대상어종), 설명·이미지·희귀도 등은 이후 큐레이션으로 확장한다. catch-all {@code
 * 기타어종} 포함 여부·도감 대상 여부는 📋 TBD → docs/spec.md, docs/external.md §1.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "fishes")
public class Fish extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 어종명. 고유값이라 (스팟,어종) 페어 시드 매핑 기준 키로 사용한다. */
  @Column(nullable = false, unique = true)
  private String name;

  /** 어종 설명(TBD). */
  @Column(columnDefinition = "TEXT")
  private String description;

  /** 서식지(TBD). */
  private String habitat;

  /** 보호종 여부. 기본 false. */
  @Column(nullable = false)
  private boolean isProtection;

  /** 도감 이미지 URL(S3, TBD). */
  private String imageUrl;

  /** 희귀도(TBD). */
  @Enumerated(EnumType.STRING)
  private Rarity rarity;
}
