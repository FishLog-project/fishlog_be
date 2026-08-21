package com.fishlog.fishlog_be.domain.spot.controller;

import com.fishlog.fishlog_be.domain.spot.dto.SpotDetailResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
                                { "id": 1, "name": "가거도", "lat": 34.07308, "lot": 125.08805, "category": "해양" }
                              ]
                            }
                            """)))
  })
  BaseResponse<List<SpotResponse>> getSpots();

  @Operation(
      summary = "낚시 스팟 상세",
      description =
          """
          ### 설명
          - 스팟 기본정보(위치명·좌표·금지 여부) + 주요 대상 어종 + **실시간 예보**(파고·수온·기온·유속·풍속·물때·낚시지수)를 병합해 반환합니다.
          - 예보는 저장하지 않고 바다낚시지수 API를 호출해 Redis에 반나절(12h) 캐시한 뒤 스팟명으로 필터해 서빙합니다.
          - `forecast`는 **오늘 날짜(KST) + 현재 시각의 오전/오후 1건**입니다(오전 00~11시=오전, 12시~=오후). 단일 객체이며 없으면 `null`.

          ### 제약조건
          - 경로 변수 `spotId`: 존재하는 스팟이어야 함(없으면 404).

          ### 예보 미제공(`forecast: null`)
          - 예보 외부 호출 실패·타임아웃, 매칭 예보 없음(담수 스팟 등), 또는 **오늘·현재 시간대 예보가 없을 때** `forecast`는 `null`입니다. 이 경우에도 기본정보·대상 어종은 정상 200으로 응답합니다.

          ### ⚠ 예외상황
          - `SPOT_NOT_FOUND(404)`: 해당 id의 스팟이 없는 경우
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공(예보 포함/미포함)",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SpotDetailResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "spotId": 1,
                                "name": "가거도",
                                "lat": 34.07308,
                                "lot": 125.08805,
                                "prohibit": false,
                                "category": "해양",
                                "majorFishes": ["감성돔", "참돔"],
                                "forecast": {
                                  "predcYmd": "2026-08-19",
                                  "predcNoonSeCd": "오전",
                                  "totalIndex": "보통",
                                  "tdlvHrCn": "중조기",
                                  "minWvhgt": 0.5, "maxWvhgt": 1.0,
                                  "minWtem": 18.0, "maxWtem": 21.0,
                                  "minArtmp": 20.0, "maxArtmp": 26.0,
                                  "minCrsp": 0.1, "maxCrsp": 0.6,
                                  "minWspd": 2.0, "maxWspd": 5.0
                                }
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "스팟을 찾을 수 없음(S001)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 404, "message": "스팟을 찾을 수 없습니다.", "data": null }
                            """)))
  })
  BaseResponse<SpotDetailResponse> getSpotDetail(@Parameter(example = "1") Long spotId);
}
