package com.fishlog.fishlog_be.domain.ranking.controller;

import com.fishlog.fishlog_be.domain.ranking.dto.RankingResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 사용자 랭킹 API Swagger 문서(문서 전용). 완성도·크기 두 기준을 경로로 구분한다. → docs/architecture.md, docs/ranking.md
 *
 * <p>매핑/바인딩 애너테이션은 두지 않는다. 실행부는 {@link RankingController} 참고.
 *
 * <p><b>공통 응답 구조:</b> 두 랭킹은 같은 {@link RankingResponse} 구조를 공유하고 `metric`으로 구분한다.
 *
 * <ul>
 *   <li>`metric`: `COMPLETION`(완성도) | `SIZE`(크기)
 *   <li>`totalFishCount`: 완성도 분모(전체 도감 어종 수). <b>크기 랭킹에서는 null</b>
 *   <li>`me`: 본인 순위 블록. <b>로그인(Bearer 토큰) 시에만</b> 채워지고, 비로그인 요청이면 null
 *   <li>`rankings`: 전체 순위(점수 내림차순)
 * </ul>
 *
 * <p><b>인증:</b> 랭킹 목록은 공개(비로그인 조회 가능)다. `Authorization: Bearer {accessToken}`를 함께 보내면 그 사용자의 내
 * 순위(`me`)가 추가로 채워진다(토큰 선택).
 *
 * <p><b>순위 규칙:</b> 공동 순위를 부여한다(예: 점수 [93.1, 93.1, 86.2] → rank [1, 1, 3]). 로그인했지만 인증 기록이 전혀 없으면
 * `me.rank`는 null이다.
 *
 * <p><b>RankingEntryResponse 필드(기준별로 쓰는 점수만 채워지고 나머지는 null):</b>
 *
 * <ul>
 *   <li>완성도: `caughtCount`, `completionRate` 사용 (`maxSize`=null)
 *   <li>크기: `maxSize` 사용 (`caughtCount`·`completionRate`=null)
 *   <li>`nickname`: `users`에서 조회해 채운다(사용자를 찾지 못하면 null) → docs/ranking.md
 * </ul>
 */
@Tag(name = "Ranking", description = "사용자 랭킹(도감 완성도·최대 크기) API")
public interface RankingControllerSpec {

  @Operation(
      summary = "도감 완성도 랭킹",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 고유 수집대상 어종 수 ÷ 전체 도감 어종 수(`completionRate`, %) 기준 순위입니다.
          - `metric="COMPLETION"`, `totalFishCount`(분모)가 채워집니다.
          - 각 순위 항목은 `caughtCount`(인증한 고유 어종 수)와 `completionRate`(소수 1자리 %)를 사용합니다.

          ### 사용 방법
          - `GET /api/rankings/completion` : 전체 순위만 필요할 때(본인 순위 없음, 비로그인 가능).
          - `GET /api/rankings/completion` + 헤더 `Authorization: Bearer {accessToken}` : 본인 순위(`me`)를 함께 받고 싶을 때.

          ### 제약조건
          - 목록은 공개(인증 불필요)입니다.
          - 토큰은 **선택**입니다(내 순위 계산용). 토큰이 없으면 `me`는 null입니다.

          ### me(본인 순위) 처리
          - 로그인 사용자가 인증 기록을 가지면 전체 순위에서 본인 항목을 그대로 내려줍니다.
          - 로그인했으나 인증 기록이 없으면 `me`는 `rank:null`, `caughtCount:0`, `completionRate:0.0`으로 내려갑니다.

          ### ⚠ 예외상황
          - `401`: 토큰을 보냈으나 무효한 경우(토큰을 아예 보내지 않으면 401이 아니라 `me:null`로 정상 처리).
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
                              "data": {
                                "metric": "COMPLETION",
                                "totalFishCount": 29,
                                "me": {
                                  "rank": 3,
                                  "userId": 1,
                                  "nickname": "초보강태공",
                                  "caughtCount": 20,
                                  "completionRate": 69.0,
                                  "maxSize": null
                                },
                                "rankings": [
                                  {
                                    "rank": 1,
                                    "userId": 7,
                                    "nickname": "낚시왕",
                                    "caughtCount": 27,
                                    "completionRate": 93.1,
                                    "maxSize": null
                                  },
                                  {
                                    "rank": 1,
                                    "userId": 4,
                                    "nickname": "바다사랑",
                                    "caughtCount": 27,
                                    "completionRate": 93.1,
                                    "maxSize": null
                                  },
                                  {
                                    "rank": 3,
                                    "userId": 1,
                                    "nickname": "초보강태공",
                                    "caughtCount": 20,
                                    "completionRate": 69.0,
                                    "maxSize": null
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "토큰을 보냈으나 무효",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 401,
                              "message": "인증이 필요합니다.",
                              "data": null
                            }
                            """)))
  })
  BaseResponse<RankingResponse> getCompletionRanking(@Parameter(hidden = true) Long userId);

  @Operation(
      summary = "최대 어종 크기 랭킹",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 잡은 어종 중 최대 크기(cm, `maxSize`) 기준 순위입니다.
          - `metric="SIZE"`, `totalFishCount`는 **null**(완성도 전용 분모이므로 크기 랭킹에서는 사용 안 함).
          - 각 순위 항목은 `maxSize`만 사용하고 `caughtCount`·`completionRate`는 null입니다.

          ### 사용 방법
          - `GET /api/rankings/size` : 전체 순위만 필요할 때(비로그인 가능).
          - `GET /api/rankings/size` + 헤더 `Authorization: Bearer {accessToken}` : 본인 순위(`me`)를 함께 받고 싶을 때.

          ### 제약조건
          - 목록은 공개(인증 불필요)입니다.
          - 토큰은 **선택**입니다(내 순위 계산용). 토큰이 없으면 `me`는 null입니다.

          ### me(본인 순위) 처리
          - 로그인 사용자가 인증 기록을 가지면 전체 순위에서 본인 항목을 내려줍니다.
          - 로그인했으나 인증 기록이 없으면 `me`는 `rank:null`, `maxSize:null`로 내려갑니다.

          ### ⚠ 예외상황
          - `401`: 토큰을 보냈으나 무효한 경우(토큰을 아예 보내지 않으면 401이 아니라 `me:null`로 정상 처리).
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
                              "data": {
                                "metric": "SIZE",
                                "totalFishCount": null,
                                "me": {
                                  "rank": 2,
                                  "userId": 1,
                                  "nickname": "초보강태공",
                                  "caughtCount": null,
                                  "completionRate": null,
                                  "maxSize": 74.5
                                },
                                "rankings": [
                                  {
                                    "rank": 1,
                                    "userId": 7,
                                    "nickname": "낚시왕",
                                    "caughtCount": null,
                                    "completionRate": null,
                                    "maxSize": 88.0
                                  },
                                  {
                                    "rank": 2,
                                    "userId": 1,
                                    "nickname": "초보강태공",
                                    "caughtCount": null,
                                    "completionRate": null,
                                    "maxSize": 74.5
                                  },
                                  {
                                    "rank": 3,
                                    "userId": 4,
                                    "nickname": "바다사랑",
                                    "caughtCount": null,
                                    "completionRate": null,
                                    "maxSize": 61.0
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "토큰을 보냈으나 무효",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 401,
                              "message": "인증이 필요합니다.",
                              "data": null
                            }
                            """)))
  })
  BaseResponse<RankingResponse> getSizeRanking(@Parameter(hidden = true) Long userId);
}
