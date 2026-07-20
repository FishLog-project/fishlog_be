package com.fishlog.fishlog_be.domain.fish.controller;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 전체 도감(마스터 어종 카탈로그) 공개 조회 API Swagger 문서(문서 전용). → docs/architecture.md, docs/spec.md
 *
 * <p>매핑/바인딩 애너테이션은 두지 않는다. 실행부는 {@link FishController} 참고.
 */
@Tag(name = "Fish", description = "전체 도감(어종 카탈로그) API")
public interface FishControllerSpec {

  @Operation(
      summary = "전체 도감 목록 조회 / 이름 검색",
      description =
          """
          ### 설명
          - 수집 대상 어종(`isCollectible=true`) 목록과 총 수(`totalCount`)를 반환합니다.
          - 도감 그리드/썸네일 화면용 요약 정보(id·name·imageUrl·rarity)입니다.
          - 정렬은 어종 ID 오름차순으로 고정됩니다.

          ### 사용 방법
          - `GET /api/fish` : 전체 수집 대상 어종 목록.
          - `GET /api/fish?name=감성돔` : 어종명 **완전일치** 검색.
            - `name`은 부분검색/유사검색이 아니라 정확히 일치하는 1건을 찾습니다.
            - 일치하는 어종이 없으면 **에러가 아니라** `200` + `totalCount:0` + 빈 목록을 반환합니다.

          ### 제약조건
          - 공개 API(인증 불필요).
          - `name`은 선택 파라미터입니다. 빈 문자열/공백은 미전달과 동일하게 전체 목록으로 취급합니다.

          ### rarity(희귀도) enum
          - `LOW` · `USUALLY` · `HIGH`

          ### ⚠ 예외상황
          - 이 엔드포인트는 조회 실패를 도메인 예외로 던지지 않습니다(검색 0건도 정상 200).
          - 예기치 못한 서버 오류 시에만 `500`이 반환됩니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공(검색 0건 포함)",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "전체 목록",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": {
                              "totalCount": 2,
                              "fishes": [
                                {
                                  "id": 1,
                                  "name": "감성돔",
                                  "imageUrl": "https://.../fish/1.png",
                                  "rarity": "USUALLY"
                                },
                                {
                                  "id": 2,
                                  "name": "참돔",
                                  "imageUrl": "https://.../fish/2.png",
                                  "rarity": "HIGH"
                                }
                              ]
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "이름 검색 0건",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": { "totalCount": 0, "fishes": [] }
                          }
                          """)
                }))
  })
  BaseResponse<FishListResponse> getFishList(
      @Parameter(description = "어종명 완전일치 검색(선택). 예: 감성돔", example = "감성돔") String name);

  @Operation(
      summary = "어종 상세 조회",
      description =
          """
          ### 설명
          - 어종 ID로 도감 상세 정보(설명·서식지·이미지·희귀도)를 반환합니다.

          ### 사용 방법
          - `GET /api/fish/{id}` (예: `/api/fish/1`).
          - 목록 응답의 `data.fishes[].id`를 그대로 경로 변수로 사용합니다.

          ### 제약조건
          - 공개 API(인증 불필요).
          - `id`: 존재하는 수집 대상 어종이어야 합니다.

          ### ⚠ 예외상황
          - `FISH_NOT_FOUND(404)` (`F001`): 해당 id의 수집 대상 어종이 없는 경우.
          - `400`: `id`가 숫자가 아닌 등 경로 변수 타입이 맞지 않는 경우(`GlobalErrorCode`, 공통 처리).
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
                                "id": 1,
                                "name": "감성돔",
                                "description": "연안 방파제에서 흔히 잡히는 돔.",
                                "habitat": "남해 연안",
                                "imageUrl": "https://.../fish/1.png",
                                "rarity": "USUALLY"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "어종을 찾을 수 없음(F001)",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 404,
                              "message": "해당 어종을 찾을 수 없습니다.",
                              "data": null
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "경로 변수 타입 오류",
        content =
            @Content(
                mediaType = "application/json",
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": false,
                              "code": 400,
                              "message": "요청 파라미터가 올바르지 않습니다.",
                              "data": null
                            }
                            """)))
  })
  BaseResponse<FishDetailResponse> getFishDetail(@Parameter(example = "1") Long id);
}
