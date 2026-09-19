package com.fishlog.fishlog_be.domain.tour.dto;

import com.fishlog.fishlog_be.domain.tour.entity.TourCategory;
import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 주변 관광 장소 목록(카테고리·페이지 단위) 응답.
 *
 * @param type 카테고리 라벨(관광지/숙박/음식점)
 * @param page 현재 페이지(1-base)
 * @param numOfRows 페이지당 개수(고정 30)
 * @param totalCount 반경 내 전체 건수
 * @param hasNext 다음 페이지 존재 여부
 * @param items 이번 페이지 장소 목록(거리순)
 */
@Schema(description = "주변 관광 장소 목록")
public record NearbyTourResponse(
    @Schema(example = "음식점") String type,
    @Schema(example = "1") int page,
    @Schema(example = "30") int numOfRows,
    @Schema(example = "128") int totalCount,
    @Schema(example = "true") boolean hasNext,
    List<TourSpotResponse> items) {

  /**
   * @param items 혼잡도까지 조립이 끝난 장소 목록(거리순). 혼잡도 결합은 외부 조회가 얽혀 있어 서비스가 맡고, 이 DTO는 페이지 메타만 계산한다.
   */
  public static NearbyTourResponse of(
      TourCategory category, TourApiResult result, List<TourSpotResponse> items) {
    boolean hasNext = (long) result.pageNo() * result.numOfRows() < result.totalCount();
    return new NearbyTourResponse(
        category.label(), result.pageNo(), result.numOfRows(), result.totalCount(), hasNext, items);
  }
}
