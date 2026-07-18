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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 어종 — 도감 기준 데이터. ERD v0.1 기준.
 *
 * <p>{@code name}은 스팟 시드({@code SpotSeedLoader})가, {@code description}·{@code habitat}은 콘텐츠
 * 시드({@code FishContentSeedLoader}, {@code data/fish/fish_content_seed.json})가 채운다. {@code
 * imageUrl}·{@code rarity}는 아직 큐레이션 전이라 null 이다. → docs/spec.md, docs/external.md §1
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

  /** 도감 이미지 URL(S3, TBD). */
  private String imageUrl;

  /** 희귀도(TBD). */
  @Enumerated(EnumType.STRING)
  private Rarity rarity;

  /**
   * 도감(수집) 대상 여부. 기본 {@code true}이며, catch-all {@code 기타어종}·placeholder 등 도감에 노출하지 않을 항목만 시드에서
   * {@code false}로 저장한다. 전체 도감 조회는 이 플래그로 필터한다. → docs/spec.md
   */
  @Column(nullable = false)
  @Builder.Default
  private boolean isCollectible = true;

  /**
   * 도감 콘텐츠(설명·서식지)를 채운다. 시드 로더({@code FishContentSeedLoader})가 사용하며, 엔티티에 setter 를 열지 않기 위한 도메인
   * 메서드다. 적용 여부 판단은 호출부(로더)의 책임이다.
   */
  public void applyContent(String description, String habitat) {
    this.description = description;
    this.habitat = habitat;
  }
}
