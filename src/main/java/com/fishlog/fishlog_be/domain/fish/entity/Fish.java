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
import org.hibernate.annotations.ColumnDefault;

/**
 * 어종 — 도감 기준 데이터. ERD v0.1 기준.
 *
 * <p>{@code name}은 스팟 시드({@code SpotSeedLoader})가, {@code description}·{@code habitat}·{@code
 * rarity}는 콘텐츠 시드({@code FishContentSeedLoader}, {@code data/fish/fish_content_seed.json})가 채운다.
 * {@code imageUrl}은 아직 큐레이션 전이라 null 이다.
 *
 * <p><b>이 테이블의 모든 행이 곧 전체 도감이다.</b> 콘텐츠 시드에 없는 어종은 로더가 물리 삭제하므로 "도감에서 숨긴 어종"이라는 상태가 존재하지 않는다(과거의
 * {@code is_collectible} 플래그는 제거됨). → docs/spec.md, docs/external.md §1
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
   * 제철(계절) — 어종이 잘 잡히는 계절. 한 어종이 여러 계절에 제철일 수 있어 계절별 boolean 4컬럼으로 둔다(다중값). 콘텐츠 시드가 채운다.
   * {@code @ColumnDefault("false")}로 기존 행도 false로 채워진다(ddl-auto=update). 배너 "계절별 추천 어종"에 사용. →
   * docs/spec.md
   */
  @Column(name = "season_spring", nullable = false)
  @ColumnDefault("false")
  private boolean springSeason;

  @Column(name = "season_summer", nullable = false)
  @ColumnDefault("false")
  private boolean summerSeason;

  @Column(name = "season_fall", nullable = false)
  @ColumnDefault("false")
  private boolean fallSeason;

  @Column(name = "season_winter", nullable = false)
  @ColumnDefault("false")
  private boolean winterSeason;

  /**
   * 도감 콘텐츠(설명·서식지·희귀도)를 채운다. 시드 로더({@code FishContentSeedLoader})가 사용하며, 엔티티에 setter 를 열지 않기 위한 도메인
   * 메서드다. 적용 여부 판단은 호출부(로더)의 책임이다.
   */
  public void applyContent(String description, String habitat, Rarity rarity) {
    this.description = description;
    this.habitat = habitat;
    this.rarity = rarity;
  }

  /** 제철(계절) 플래그를 채운다. 콘텐츠 시드 로더가 사용한다. */
  public void applySeasons(
      boolean springSeason, boolean summerSeason, boolean fallSeason, boolean winterSeason) {
    this.springSeason = springSeason;
    this.summerSeason = summerSeason;
    this.fallSeason = fallSeason;
    this.winterSeason = winterSeason;
  }
}
