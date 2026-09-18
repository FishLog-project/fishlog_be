package com.fishlog.fishlog_be.domain.tour.controller;

import com.fishlog.fishlog_be.domain.tour.dto.NearbyTourResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Tour API", description = "주변 관광 정보 관련 API")
public interface TourControllerSpec {

  @Operation(
      summary = "주변 관광 장소 조회",
      description =
          """
          ### 설명
          - 사용자 현재 위치(위도·경도) 기준 **반경 내 관광 장소**를 거리순으로 조회합니다.
          - 한국관광공사 TourAPI(위치기반 관광정보)를 실시간 호출합니다(별도 저장 없음).
          - **페이지당 30개 고정**이며 `page`로 다음 페이지를 조회합니다.

          ### 카테고리(type)
          - `관광지` · `숙박` · `음식점` (한글). 그 외 값은 400.

          ### 제약조건
          - `type`·`lat`·`lng` 필수. `radius`(m, 기본 5000, 최대 20000)·`page`(기본 1)는 선택.
          - 인증 불필요(공개).
          - 이미지·상세주소가 없는 장소는 `firstImage`/`firstImage2`/`addr2`가 `null`입니다.

          ### 당일 혼잡도(congestion)
          - `type=관광지`일 때, 각 항목에 **그날 예상 혼잡도**를 함께 내려줍니다.
            한국관광공사 관광지 집중률 방문자 추이 예측(data.go.kr 15128555)을 장소명으로 조인합니다.
          - `rate`: 집중률 지수 **0~100**. 퍼센트가 아니라 2018년 이후 피크를 100으로 정규화한 상대 지수입니다.
          - `level`: `여유`(50 미만) / `보통`(50~80) / `혼잡`(80 이상).
          - `baseDate`: 예측 기준일(조회 당일, `yyyy-MM-dd`).
          - **`congestion`은 `null`일 수 있습니다.** 화면은 이때 혼잡도 칩을 숨기세요(0으로 표시 금지).
            - `숙박`·`음식점` — 집중률 데이터셋이 관광지만 다룸(항상 `null`)
            - 데이터셋에 없는 장소 — 항·포구(격포항 등)·리조트·신규시설 다수. 관광지 기준 적중률 약 57%
            - **전남 전역** — 데이터셋에 해당 지역이 없음
            - 집중률 API 장애·쿼터 소진 — 이 경우에도 목록 응답은 **200**을 유지합니다

          ### ⚠ 예외상황
          - `INVALID_TYPE(400)`: 지원하지 않는 카테고리(`type`)
          - `TOUR_API_ERROR(502)`: TourAPI 응답 오류(쿼터 초과·비정상 응답 등)
          - `TOUR_API_UNAVAILABLE(503)`: TourAPI 연결 실패·타임아웃
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                schema = @Schema(implementation = NearbyTourResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "type": "관광지",
                                "page": 1,
                                "numOfRows": 30,
                                "totalCount": 41,
                                "hasNext": true,
                                "items": [
                                  {
                                    "title": "채석강",
                                    "firstImage": "http://tong.visitkorea.or.kr/cms/image1.jpg",
                                    "firstImage2": "http://tong.visitkorea.or.kr/cms/thumb1.jpg",
                                    "addr1": "전북특별자치도 부안군 변산면 격포리",
                                    "addr2": null,
                                    "mapX": 126.4699,
                                    "mapY": 35.6204,
                                    "congestion": {
                                      "rate": 39.6,
                                      "level": "여유",
                                      "baseDate": "2026-09-19"
                                    }
                                  },
                                  {
                                    "title": "격포항",
                                    "firstImage": "http://tong.visitkorea.or.kr/cms/image2.jpg",
                                    "firstImage2": "http://tong.visitkorea.or.kr/cms/thumb2.jpg",
                                    "addr1": "전북특별자치도 부안군 변산면 격포리",
                                    "addr2": null,
                                    "mapX": 126.4633,
                                    "mapY": 35.6167,
                                    "congestion": null
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "지원하지 않는 카테고리",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "지원하지 않는 관광 카테고리입니다. (관광지/숙박/음식점)", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "502",
        description = "TourAPI 응답 오류",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 502, "message": "관광 정보 조회에 실패했습니다.", "data": null }
                            """)))
  })
  BaseResponse<NearbyTourResponse> getNearbyTours(
      @Schema(description = "카테고리(관광지/숙박/음식점)", example = "관광지") String type,
      @Schema(description = "위도", example = "35.1587") double lat,
      @Schema(description = "경도", example = "129.1603") double lng,
      @Schema(description = "반경(m), 기본 5000, 최대 20000", example = "5000") int radius,
      @Schema(description = "페이지(1-base), 기본 1", example = "1") int page);
}
