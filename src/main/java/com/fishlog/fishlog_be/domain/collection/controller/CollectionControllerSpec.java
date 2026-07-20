package com.fishlog.fishlog_be.domain.collection.controller;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 사용자 도감(어종 인증) 조회 API Swagger 문서(문서 전용). → docs/architecture.md, docs/spec.md
 *
 * <p>매핑/바인딩 애너테이션은 두지 않는다. 실행부는 {@link CollectionController} 참고.
 *
 * <p><b>userId는 인증(JWT) 도입 전 임시 파라미터</b>다. 로그인 완성 후 로그인 사용자로 대체되며, 그때 이 문서의 파라미터도 함께 정리한다. →
 * docs/auth-followup.md
 */
@Tag(name = "Collection", description = "사용자 도감(어종 인증) API")
public interface CollectionControllerSpec {

  @Operation(
      summary = "내 어종 인증 조회",
      description =
          """
          ### 설명
          - 특정 어종에 대해 특정 사용자가 인증한 사진 목록(`imageUrls`)과 잡은 횟수(`catchCount`)를 반환합니다.
          - 어종 상세 화면에서 "내가 이 물고기를 몇 번, 어떤 사진으로 잡았는지"를 보여줄 때 사용합니다.
          - `catchCount`는 별도 저장값이 아니라 인증 기록 수에서 파생됩니다(= `imageUrls.length`).

          ### 사용 방법
          - `GET /api/collections?userId={userId}&fishId={fishId}`
            - 예: `GET /api/collections?userId=1&fishId=1`
          - `fishId`는 전체 도감(`GET /api/fish`) 응답의 어종 id를 사용합니다.

          ### 제약조건
          - `userId`, `fishId` **모두 필수** 쿼리 파라미터입니다.
          - 아직 잡지 않은 어종이어도 **에러가 아닙니다** → `200` + `catchCount:0` + 빈 목록(`imageUrls:[]`).

          ### ⚠ 예외상황
          - `400`: `userId` 또는 `fishId`가 누락되었거나 숫자가 아닌 경우(공통 파라미터 검증, `GlobalErrorCode`).
          - 존재하지 않는 `userId`/`fishId`를 넘겨도 현재는 404가 아니라 빈 결과(`catchCount:0`)를 반환합니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공(미인증 어종 포함)",
        content =
            @Content(
                mediaType = "application/json",
                examples = {
                  @ExampleObject(
                      name = "인증 기록 있음",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": {
                              "catchCount": 2,
                              "imageUrls": [
                                "https://.../catch/10.png",
                                "https://.../catch/11.png"
                              ]
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "아직 안 잡은 어종",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": { "catchCount": 0, "imageUrls": [] }
                          }
                          """)
                })),
    @ApiResponse(
        responseCode = "400",
        description = "필수 파라미터 누락/타입 오류",
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
  BaseResponse<CatchRecordResponse> getMyCatch(
      @Parameter(description = "사용자 id(임시). 추후 로그인 토큰으로 대체", example = "1") Long userId,
      @Parameter(description = "전체 도감 어종 id", example = "1") Long fishId);

  @Operation(
      summary = "내 도감 조회",
      description =
          """
          ### 설명
          - 전체 수집 대상 어종을 도감 순서(어종 ID 오름차순)대로 반환하며, 각 칸에 내가 잡았는지(`caught`)를 표시합니다.
          - `caught=true`면 도감 이미지를, `false`면 같은 이미지를 그림자(실루엣)로 렌더하도록 프론트가 분기합니다(그림자는 클라이언트 이펙트, 서버는 플래그만 내려줌).
          - `totalCount`(전체 수집 대상 수)와 `caughtCount`(내가 잡은 수)로 도감 완성도를 함께 계산할 수 있어, 별도 조회 없이 진행도 바를 그릴 수 있습니다. → docs/ranking.md

          ### 사용 방법
          - `GET /api/collections/dex?userId={userId}`
            - 예: `GET /api/collections/dex?userId=1`

          ### 제약조건
          - `userId` **필수** 쿼리 파라미터입니다.
          - `fishes` 배열의 순서·집합은 `GET /api/fish` 전체 도감과 동일합니다(잡은 어종 여부만 덧입힘).

          ### rarity(희귀도) enum
          - `LOW` · `USUALLY` · `HIGH`

          ### ⚠ 예외상황
          - `400`: `userId` 누락 또는 숫자가 아닌 경우(공통 파라미터 검증).
          - 인증 기록이 하나도 없는 사용자여도 정상 `200`(모든 칸 `caught:false`, `caughtCount:0`).
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
                                "totalCount": 2,
                                "caughtCount": 1,
                                "fishes": [
                                  {
                                    "id": 1,
                                    "name": "감성돔",
                                    "imageUrl": "https://.../fish/1.png",
                                    "rarity": "USUALLY",
                                    "caught": true
                                  },
                                  {
                                    "id": 2,
                                    "name": "참돔",
                                    "imageUrl": "https://.../fish/2.png",
                                    "rarity": "HIGH",
                                    "caught": false
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "필수 파라미터 누락/타입 오류",
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
  BaseResponse<MyDexResponse> getMyDex(
      @Parameter(description = "사용자 id(임시). 추후 로그인 토큰으로 대체", example = "1") Long userId);
}
