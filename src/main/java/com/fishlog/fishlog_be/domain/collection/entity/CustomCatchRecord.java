package com.fishlog.fishlog_be.domain.collection.entity;

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
 * 도감 외 어종 인증 기록(사용자가 직접 등록한 물고기 1건). {@link CustomFish}(사용자별 어종)에 대해 <b>등록 1건 = 1행</b>으로, {@code
 * fishes}↔{@code catch_record} 관계를 그대로 옮긴 구조다.
 *
 * <p><b>왜 {@link CatchRecord}를 확장하지 않고 별도 테이블인가:</b> {@code catch_record.fishes_id}는 NOT NULL FK라
 * 도감 24종 밖 물고기를 담을 수 없다. 그렇다고 FK를 nullable 로 풀면 {@code fishes_id IS NULL}인 행이 랭킹 집계(완성도 {@code
 * COUNT(DISTINCT fish.id)} · 크기 {@code MAX(size)})와 도감 그리드 판정에 섞여 들어간다. 테이블을 나누면 그 쿼리들을 한 줄도 건드리지
 * 않고 <b>검증되지 않은 수기 어종이 랭킹·도감 완성도에서 자동으로 빠진다</b>. → docs/spec.md
 *
 * <p>{@code userId}는 {@code custom_fish}에도 있어 중복처럼 보이지만 그대로 둔다 — 회원탈퇴 정리({@code deleteByUserId})가
 * 조인 없이 끝나고, {@code catch_record}와 신원 컬럼 형태가 같아 나중에 {@code @ManyToOne User} 승격을 한 번에 처리할 수 있다. →
 * docs/auth-followup.md §1
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "custom_catch_record")
public class CustomCatchRecord extends BaseTimeEntity {

  /**
   * 잡은 위치 수기 입력의 최대 길이(자).
   *
   * <p>{@link CatchRecord#MAX_LOCATION_LENGTH}와 같은 값을 참조한다 — 같은 화면에서 같은 입력란으로 받는 값이라, 어느 쪽으로 등록하느냐에
   * 따라 허용 길이가 달라지면 안 된다.
   */
  public static final int MAX_LOCATION_LENGTH = CatchRecord.MAX_LOCATION_LENGTH;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 등록한 사용자 id. {@code users.id}를 가리키지만 FK 관계는 아직 아니다(plain Long). */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /** 이 기록이 속한 사용자별 어종. 어종명·서식지는 여기(어종)에 있고 기록에는 중복 저장하지 않는다. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "custom_fish_id", nullable = false)
  private CustomFish customFish;

  /** 인증 사진 S3 URL(1건, {@code custom-fish/} 경로). */
  @Column(name = "certified_image_url", nullable = false)
  private String certifiedImageUrl;

  /**
   * 잡은 어종 크기(cm, 필수).
   *
   * <p>{@code catch_record.size}와 달리 <b>크기 랭킹에는 반영되지 않는다</b> — 어종명이 검증되지 않은 기록이라 랭킹 쿼리가 이 테이블을 보지
   * 않는다. 그래도 NOT NULL 로 받는 이유는 기록 자체의 가치(내가 몇 cm 짜리를 잡았는지)가 크기에 있기 때문이다.
   */
  @Column(nullable = false)
  private Double size;

  /**
   * 잡은 위치(사용자 수기 입력, 선택).
   *
   * <p>{@link CatchRecord#getCatchLocation()}과 동일한 규칙이다 — 등록 스팟 선택이 아니라 자유 텍스트이고, 공백만 입력한 경우는 서비스에서
   * {@code null}로 정규화해 "빈 문자열"과 "미입력"이 섞이지 않게 한다.
   *
   * <p>서식지({@link CustomFish#getHabitat()})와 다른 값이다 — 이쪽은 "이번에 어디서 잡았나"(기록의 속성), 저쪽은 "이 물고기가 원래 어디
   * 사나"(어종의 속성)다.
   */
  @Column(name = "catch_location", length = MAX_LOCATION_LENGTH)
  private String catchLocation;
}
