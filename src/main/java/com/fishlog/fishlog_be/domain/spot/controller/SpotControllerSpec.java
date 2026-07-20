package com.fishlog.fishlog_be.domain.spot.controller;

import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

/** 낚시 스팟 API Swagger 문서(문서 전용). → docs/architecture.md, docs/spec.md */
@Tag(name = "Spot API", description = "낚시 스팟 관련 API")
public interface SpotControllerSpec {

  @Operation(
      summary = "낚시 스팟 목록",
      description =
          """
          ### 설명
          - 지도 마커용 낚시 스팟 목록(id·name·lat·lot)을 전체 반환합니다.
          - 프론트(카카오맵)가 이 좌표로 마커를 표시합니다.

          ### 제약조건
          - 없음(공개 API).
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": [
                                { "id": 1, "name": "가거도", "lat": 34.07308, "lot": 125.08805 }
                              ]
                            }
                            """)))
  })
  BaseResponse<List<SpotResponse>> getSpots();
}
