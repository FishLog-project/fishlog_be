package com.fishlog.fishlog_be.domain.banner.controller;

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
}
