package com.fishlog.fishlog_be.domain.spot.entity;

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
 * 낚시 스팟 — 바다낚시지수 API(15142486)에서 수집한 **불변 정보**만 저장한다.
 *
 * <p>낚시지수·날씨·물때 등 예보성 데이터는 저장하지 않고 상세 조회 시 실시간 호출한다. → docs/spec.md "스팟 데이터 설계", docs/geo.md
 *
 * <p>좌표는 ERD v0.1 상 FLOAT 이나, 위경도 정밀도(소수 5자리) 보존을 위해 {@code double} 로 매핑한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "spots")
public class Spot extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 위치명(seafsPstnNm). 고유값이라 시드 upsert 기준 키로 사용한다. */
  @Column(nullable = false, unique = true)
  private String name;

  /** 위도(API lat). */
  @Column(nullable = false)
  private double lat;

  /** 경도(API lot). */
  @Column(nullable = false)
  private double lot;

  /** 낚시 금지 여부(서비스 운영값, API 아님). 기본 false. */
  @Column(nullable = false)
  private boolean prohibit;
}
