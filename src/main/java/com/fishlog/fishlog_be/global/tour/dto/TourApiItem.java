package com.fishlog.fishlog_be.global.tour.dto;

/**
 * TourAPI 관광 장소 1건(정규화 완료). 빈 문자열은 {@code null}로, 좌표는 {@code Double}로 파싱된 상태다.
 *
 * <p><b>법정동 코드 두 필드는 관광지 집중률(혼잡도) 조회의 조인 키다.</b> 같은 응답의 {@code areacode}/{@code sigungucode}는
 * 한국관광공사 자체 코드라 집중률 API가 받지 않는다. 집중률 API가 요구하는 {@code signguCd}(5자리)는 {@code lDongRegnCd +
 * lDongSignguCd}로 만든다. → docs/external.md §2
 *
 * @param title 장소명
 * @param firstImage 대표 이미지 URL (없으면 null)
 * @param firstImage2 썸네일 이미지 URL (없으면 null)
 * @param addr1 기본 주소 (없으면 null)
 * @param addr2 상세 주소 (없으면 null)
 * @param mapX 경도 (파싱 실패 시 null)
 * @param mapY 위도 (파싱 실패 시 null)
 * @param lDongRegnCd 법정동 시도코드 2자리 (예: 52=전북특별자치도, 없으면 null)
 * @param lDongSignguCd 법정동 시군구코드 뒤 3자리 (예: 800=부안군, 없으면 null)
 */
public record TourApiItem(
    String title,
    String firstImage,
    String firstImage2,
    String addr1,
    String addr2,
    Double mapX,
    Double mapY,
    String lDongRegnCd,
    String lDongSignguCd) {

  /**
   * 집중률 API의 {@code signguCd}(법정동 시군구코드 5자리)를 만든다.
   *
   * @return 시도 2자리 + 시군구 3자리, 둘 중 하나라도 없으면 {@code null}
   */
  public String legalDongSignguCode() {
    if (lDongRegnCd == null || lDongSignguCd == null) {
      return null;
    }
    return lDongRegnCd + lDongSignguCd;
  }
}
