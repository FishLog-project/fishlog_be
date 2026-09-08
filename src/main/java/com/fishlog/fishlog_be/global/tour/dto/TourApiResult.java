package com.fishlog.fishlog_be.global.tour.dto;

import java.util.List;

/**
 * TourAPI {@code locationBasedList2} 한 페이지 조회 결과. 페이지 메타(전체 건수·현재 페이지·페이지 크기)와 이번 페이지의 장소 목록을 담는다.
 *
 * @param totalCount 반경 내 전체 건수
 * @param pageNo 현재 페이지(1-base)
 * @param numOfRows 페이지당 개수
 * @param items 이번 페이지 장소 목록
 */
public record TourApiResult(int totalCount, int pageNo, int numOfRows, List<TourApiItem> items) {}
