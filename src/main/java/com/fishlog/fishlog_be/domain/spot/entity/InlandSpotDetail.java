package com.fishlog.fishlog_be.domain.spot.entity;

import com.fishlog.fishlog_be.global.common.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 내륙(담수) 스팟의 하천 제원 — 하폭·유수폭·수심의 최소/최대(단위 m).
 *
 * <p>해양 스팟이 상세 조회 때 외부 API 예보를 실시간으로 병합받는 것과 달리, 내륙 스팟의 이 값들은 <b>국립생태원 전국자연환경조사 담수어류의 실측치</b>라 변하지
 * 않는다. 그래서 예보처럼 매번 호출하지 않고 스팟·어종과 같은 <b>시드로 DB에 적재</b>한다({@code data/spot/inland_detail_seed.json}
 * → {@code InlandDetailSeedLoader}).
 *
 * <p><b>스팟과 1:1</b>이며 {@code spot_id}가 PK 이자 FK 다({@link MapsId}). 해양 스팟에는 해당 없음이라 별도 테이블로 두어
 * {@code spots}에 절반이 비는 컬럼이 생기지 않게 했다.
 *
 * <p>각 값은 <b>개별 nullable</b> 이다. 조사에서 하폭만 기록되고 유수폭·수심이 빠진 스팟이 있기 때문이다(예: 복하천·자호천·춘천호상류). 6개가 전부 비는
 * 스팟은 시드 생성 단계에서 제외되므로 이 테이블에 들어오지 않는다. → docs/spec.md "담수 스팟 상세"
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "inland_spot_detail")
public class InlandSpotDetail extends BaseTimeEntity {

  /** PK = 스팟 id. {@link MapsId}로 {@link #spot} 의 식별자를 그대로 쓴다. */
  @Id private Long spotId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "spot_id")
  private Spot spot;

  /** 하폭 최소(m) — 물이 흐르지 않는 둔치까지 포함한 하천 바닥 전체 폭. */
  private Double riverWidthMin;

  /** 하폭 최대(m). */
  private Double riverWidthMax;

  /** 유수폭 최소(m) — 실제로 물이 흐르는 구간의 폭. 하폭보다 좁다. */
  private Double flowWidthMin;

  /** 유수폭 최대(m). */
  private Double flowWidthMax;

  /** 수심 최소(m). */
  private Double depthMin;

  /** 수심 최대(m). */
  private Double depthMax;

  /** 실측치 재설정(시드 재적재 시 기존 행 갱신). setter 대신 도메인 메서드. */
  public void applyMeasures(
      Double riverWidthMin,
      Double riverWidthMax,
      Double flowWidthMin,
      Double flowWidthMax,
      Double depthMin,
      Double depthMax) {
    this.riverWidthMin = riverWidthMin;
    this.riverWidthMax = riverWidthMax;
    this.flowWidthMin = flowWidthMin;
    this.flowWidthMax = flowWidthMax;
    this.depthMin = depthMin;
    this.depthMax = depthMax;
  }
}
