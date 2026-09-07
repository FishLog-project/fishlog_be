package com.fishlog.fishlog_be.domain.banner.controller;

import com.fishlog.fishlog_be.domain.banner.dto.RecommendedSpotsResponse;
import com.fishlog.fishlog_be.domain.fish.dto.SeasonalFishResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Banner API", description = "홈 배너 관련 API")
public interface BannerControllerSpec {

  @Operation(
      summary = "계절별 추천 어종(배너)",
      description =
          """
          ### 설명
          - 서버의 현재 월(KST)이 속한 계절에 **제철인 어종 중 랜덤 3종**을 반환합니다.
          - 매 호출마다 랜덤 셔플되므로 순서·구성이 달라질 수 있습니다.
          - 응답에는 어종명과 어종 이미지 URL이 포함됩니다.

          ### 계절 기준(월)
          - 봄: 3~5월 · 여름: 6~8월 · 가을: 9~11월 · 겨울: 12~2월

          ### 제약조건
          - 인증 불필요(공개). 로그인 없이 호출 가능합니다.
          - `imageUrl`은 도감 이미지 큐레이션 전이라 현재 `null`일 수 있습니다.
          - 해당 계절 제철 어종이 3종 미만이면 있는 만큼만 반환합니다(빈 배열 가능).
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                array = @ArraySchema(schema = @Schema(implementation = SeasonalFishResponse.class)),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": [
                                { "fishId": 9, "name": "갈치", "imageUrl": null },
                                { "fishId": 3, "name": "돌돔", "imageUrl": null },
                                { "fishId": 17, "name": "쏘가리", "imageUrl": null }
                              ]
                            }
                            """)))
  })
  BaseResponse<List<SeasonalFishResponse>> getSeasonalFish();

  @Operation(
      summary = "추천 스팟(배너) — 해양·내륙 조회수 최다",
      description =
          """
          ### 설명
          - 배너에 노출할 추천 스팟입니다. 인기 스팟 Top3와 **동일 기준(누적 조회수)** 으로 뽑습니다.
          - **해양·내륙 분류를 구분**해 각 분류에서 조회수 최다 스팟을 **1곳씩** 반환합니다.
          - 각 스팟에는 좌표·분류·조회수·주요 대상 어종(`majorFishes`)이 포함됩니다.

          ### 제약조건
          - 인증 불필요(공개).
          - 해당 분류의 스팟이 하나도 없으면 그 필드(`marine`/`inland`)는 `null`입니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content =
            @Content(
                schema = @Schema(implementation = RecommendedSpotsResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "marine": {
                                  "id": 1, "name": "격포항", "lat": 35.61, "lot": 126.46,
                                  "category": "해양", "viewCount": 128, "majorFishes": ["감성돔", "참돔"]
                                },
                                "inland": {
                                  "id": 57, "name": "소양호", "lat": 37.94, "lot": 127.81,
                                  "category": "내륙", "viewCount": 84, "majorFishes": ["붕어", "잉어"]
                                }
                              }
                            }
                            """)))
  })
  BaseResponse<RecommendedSpotsResponse> getPopularSpots();
}
