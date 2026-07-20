package com.fishlog.fishlog_be.domain.spot.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 스팟-어종 매핑(주요 대상 어종). ERD의 {@code major_fish}.
 *
 * <p>대상어종은 시점 불변이라 정적 매핑으로 저장한다. (spot, fish) 조합은 UNIQUE. → docs/spec.md "스팟 데이터 설계"
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "major_fish",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_major_fish_spot_fish",
            columnNames = {"spots_id", "fishes_id"}))
public class MajorFish extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "spots_id", nullable = false)
  private Spot spot;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "fishes_id", nullable = false)
  private Fish fish;

  /** 어종 시즌(제철). API 미제공이라 TBD. */
  @Column private String season;
}
