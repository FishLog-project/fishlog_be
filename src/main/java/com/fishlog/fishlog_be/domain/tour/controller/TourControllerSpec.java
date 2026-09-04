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
                                "type": "음식점",
                                "page": 1,
                                "numOfRows": 30,
                                "totalCount": 128,
                                "hasNext": true,
                                "items": [
                                  {
                                    "title": "해운대암소갈비집",
                                    "firstImage": "http://tong.visitkorea.or.kr/cms/image1.jpg",
                                    "firstImage2": "http://tong.visitkorea.or.kr/cms/thumb1.jpg",
                                    "addr1": "부산광역시 해운대구 중동2로10번길 32-10",
                                    "addr2": null,
                                    "mapX": 129.1626,
                                    "mapY": 35.1631
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
      @Schema(description = "카테고리(관광지/숙박/음식점)", example = "음식점") String type,
      @Schema(description = "위도", example = "35.1587") double lat,
      @Schema(description = "경도", example = "129.1603") double lng,
      @Schema(description = "반경(m), 기본 5000, 최대 20000", example = "5000") int radius,
      @Schema(description = "페이지(1-base), 기본 1", example = "1") int page);
}
