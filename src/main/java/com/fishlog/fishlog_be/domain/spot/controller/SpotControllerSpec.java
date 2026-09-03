package com.fishlog.fishlog_be.domain.spot.controller;

import com.fishlog.fishlog_be.domain.spot.dto.PopularSpotResponse;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

/** 낚시 스팟 API Swagger 문서(문서 전용). → docs/architecture.md, docs/spec.md */
@Tag(name = "Spot API", description = "낚시 스팟 관련 API")
public interface SpotControllerSpec {

  @Operation(
      summary = "낚시 스팟 목록",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 지도 마커용 낚시 스팟 목록(id·name·lat·lot·category)을 전체 반환합니다.
          - 각 항목에 **로그인 사용자의 찜 여부(`isFavorite`)** 를 포함합니다.
          - 프론트(카카오맵)가 이 좌표로 마커를 표시하고, `isFavorite`로 찜 별표를 표시합니다.

          ### 제약조건
          - **보호 API** — `Authorization: Bearer {accessToken}` 필요(찜 여부 계산을 위해).

          ### ⚠ 예외상황
          - `401`: 인증 토큰 없음/무효
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
                                { "id": 1, "name": "가거도", "lat": 34.07308, "lot": 125.08805, "category": "해양", "isFavorite": true }
                              ]
                            }
                            """)))
  })
  BaseResponse<List<SpotResponse>> getSpots(@Parameter(hidden = true) Long userId);

  @Operation(
      summary = "추천 낚시 스팟 (조회수 Top 3)",
      description =
          """
          ### 설명
          - **누적 조회수(검색 후 상세 조회 시에도 조회수 증가) 상위 3개** 스팟을 조회수 내림차순으로 반환합니다.
          - 홈/배너 등의 "인기 스팟" 노출용. 공개 API(인증 불필요).
          - 스팟이 3개 미만이면 있는 만큼 반환합니다.
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
                                { "id": 1, "name": "가거도", "lat": 34.07308, "lot": 125.08805, "category": "해양", "viewCount": 128, "majorFishes": ["감성돔", "참돔"] },
                                { "id": 7, "name": "격포항", "lat": 35.61, "lot": 126.46, "category": "해양", "viewCount": 96, "majorFishes": ["농어", "우럭"] },
                                { "id": 50, "name": "갈곡천", "lat": 35.51816, "lot": 126.6797, "category": "내륙", "viewCount": 42, "majorFishes": ["붕어", "잉어", "피라미"] }
                              ]
                            }
                            """)))
  })
  BaseResponse<List<PopularSpotResponse>> getPopularSpots();

  @Operation(
      summary = "낚시 스팟 상세",
      description =
          """
          ### 설명
          - 스팟 기본정보(위치명·좌표·금지 여부) + 주요 대상 어종 + **분류별 상세**를 병합해 반환합니다.
          - **분류별 상세는 배타적입니다** — `category`에 따라 한쪽만 채워지고 다른 쪽은 `null`입니다.
            - `해양` → `forecast`(파고·수온·기온·유속·풍속·물때·낚시지수), `inlandDetail`은 `null`
            - `내륙` → `inlandDetail`(하폭·유수폭·수심, 단위 m), `forecast`는 `null`
          - 예보는 저장하지 않고 바다낚시지수 API를 호출해 Redis에 반나절(12h) 캐시한 뒤 스팟명으로 필터해 서빙합니다.
          - `forecast`는 **오늘 날짜(KST) + 현재 시각의 오전/오후 1건**입니다(오전 00~11시=오전, 12시~=오후). 단일 객체이며 없으면 `null`.
          - `inlandDetail`은 국립생태원 담수어류 조사의 **실측 저장값**이라 매 호출 동일합니다. 조사에서 빠진 항목은 개별 `null`입니다(하폭만 있고 유수폭·수심이 없는 스팟이 있습니다).
          - `viewCount`(누적 조회수): 이 상세 조회 시 **사용자/IP별 1일 1회** 비동기로 증가합니다. 값은 증가 이전 시점 기준일 수 있습니다(비동기 집계).

          ### 제약조건
          - 경로 변수 `spotId`: 존재하는 스팟이어야 함(없으면 404).

          ### 예보 미제공(`forecast: null`)
          - 예보 외부 호출 실패·타임아웃, 매칭 예보 없음, 또는 **오늘·현재 시간대 예보가 없을 때** `forecast`는 `null`입니다. 이 경우에도 기본정보·대상 어종은 정상 200으로 응답합니다.
          - **내륙 스팟은 예보 대상이 아니라 외부 호출 자체를 하지 않습니다**(항상 `null`).

          ### ⚠ 예외상황
          - `SPOT_NOT_FOUND(404)`: 해당 id의 스팟이 없는 경우
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공(해양=예보 / 내륙=하천 제원)",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = SpotDetailResponse.class),
                examples = {
                  @ExampleObject(
                      name = "해양 스팟(예보 포함)",
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
                              "viewCount": 128,
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
                              },
                              "inlandDetail": null
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "내륙 스팟(하천 제원 포함)",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": {
                              "spotId": 50,
                              "name": "갈곡천",
                              "lat": 35.51816,
                              "lot": 126.6797,
                              "prohibit": false,
                              "category": "내륙",
                              "viewCount": 42,
                              "majorFishes": ["붕어", "잉어", "피라미"],
                              "forecast": null,
                              "inlandDetail": {
                                "riverWidthMin": 70.0, "riverWidthMax": 83.0,
                                "flowWidthMin": 15.0, "flowWidthMax": 60.0,
                                "depthMin": 0.2, "depthMax": 1.5
                              }
                            }
                          }
                          """)
                })),
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
  BaseResponse<SpotDetailResponse> getSpotDetail(
      @Parameter(example = "1") Long spotId,
      @Parameter(hidden = true) Long userId,
      @Parameter(hidden = true) jakarta.servlet.http.HttpServletRequest request);
}
