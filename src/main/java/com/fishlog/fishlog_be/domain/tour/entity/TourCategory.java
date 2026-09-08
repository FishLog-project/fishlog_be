package com.fishlog.fishlog_be.domain.tour.entity;

import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.tour.TourErrorCode;

/**
 * 관광 장소 카테고리 ↔ TourAPI {@code contentTypeId} 매핑. 요청·응답의 {@code type} 값은 한글 라벨({@code
 * 관광지/숙박/음식점})이다.
 *
 * <p>낚시 도감이 다루는 3종만 노출한다(문화시설·레포츠 등 다른 콘텐츠 유형은 제외).
 */
public enum TourCategory {
  ATTRACTION("관광지", 12),
  ACCOMMODATION("숙박", 32),
  RESTAURANT("음식점", 39);

  private final String label;
  private final int contentTypeId;

  TourCategory(String label, int contentTypeId) {
    this.label = label;
    this.contentTypeId = contentTypeId;
  }

  public String label() {
    return label;
  }

  public int contentTypeId() {
    return contentTypeId;
  }

  /**
   * 요청 {@code type} 문자열(한글 라벨)을 카테고리로 변환한다.
   *
   * @throws CustomException 지원하지 않는 값이면 {@code TourErrorCode.INVALID_TYPE}
   */
  public static TourCategory from(String type) {
    if (type != null) {
      String trimmed = type.trim();
      for (TourCategory c : values()) {
        if (c.label.equals(trimmed)) {
          return c;
        }
      }
    }
    throw new CustomException(TourErrorCode.INVALID_TYPE);
  }
}
