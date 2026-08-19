package com.fishlog.fishlog_be.domain.spot.entity;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 스팟 분류 — 해양 / 내륙. 상세 조회 시 해양은 실시간 예보를 병합하고, 내륙은 예보 없이 저장 정보만 응답한다. → docs/spec.md "스팟 데이터 설계"
 *
 * <p>enum 이름이 곧 저장/응답 값(해양/내륙)이다. 시드 원본({@code spot_master.json})의 {@code category} 값 "바다"/"담수"는
 * {@link #fromLabel(String)}로 매핑한다.
 */
@Getter
@RequiredArgsConstructor
public enum SpotCategory {
  해양("바다"),
  내륙("담수");

  /** 시드 원본의 한글 라벨. */
  private final String seedLabel;

  /** 시드의 라벨("바다"/"담수")을 enum으로 매핑. 알 수 없는 값이면 예외. */
  public static SpotCategory fromLabel(String label) {
    return Arrays.stream(values())
        .filter(c -> c.seedLabel.equals(label))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("알 수 없는 스팟 category: " + label));
  }
}
