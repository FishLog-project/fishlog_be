package com.fishlog.fishlog_be.global.tour.dto;

/**
 * TourAPI 관광 장소 1건(정규화 완료). 빈 문자열은 {@code null}로, 좌표는 {@code Double}로 파싱된 상태다.
 *
 * @param title 장소명
 * @param firstImage 대표 이미지 URL (없으면 null)
 * @param firstImage2 썸네일 이미지 URL (없으면 null)
 * @param addr1 기본 주소 (없으면 null)
 * @param addr2 상세 주소 (없으면 null)
 * @param mapX 경도 (파싱 실패 시 null)
 * @param mapY 위도 (파싱 실패 시 null)
 */
public record TourApiItem(
    String title,
    String firstImage,
    String firstImage2,
    String addr1,
    String addr2,
    Double mapX,
    Double mapY) {}
