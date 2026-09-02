package com.fishlog.fishlog_be.domain.collection.controller;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.ClassifyResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.dto.VerifyResponse;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 도감(어종 인증) 조회 API Swagger 문서(문서 전용). → docs/architecture.md, docs/spec.md
 *
 * <p>매핑/바인딩 애너테이션은 두지 않는다. 실행부는 {@link CollectionController} 참고.
 *
 * <p><b>보호(인증) API</b>다. 모든 조회는 {@code Authorization: Bearer {accessToken}}의 로그인 사용자 기준이며, 신원은 토큰에서
 * 얻는다(별도 userId 파라미터 없음 — 남의 도감 조회 방지). 토큰 누락/무효 시 {@code 401}. → docs/security.md
 */
@Tag(name = "Collection", description = "사용자 도감(어종 인증) API")
public interface CollectionControllerSpec {

  @Operation(
      summary = "내 어종 인증 조회",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 특정 어종에 대해 **로그인 사용자**가 인증한 사진 목록(`imageUrls`)과 잡은 횟수(`catchCount`)를 반환합니다.
          - 어종 상세 화면에서 "내가 이 물고기를 몇 번, 어떤 사진으로 잡았는지"를 보여줄 때 사용합니다.
          - `catchCount`는 별도 저장값이 아니라 인증 기록 수에서 파생됩니다(= `imageUrls.length`).

          ### 사용 방법
          - `GET /api/collections?fishId={fishId}` + 헤더 `Authorization: Bearer {accessToken}`
            - 예: `GET /api/collections?fishId=1`
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).
          - `fishId`는 전체 도감(`GET /api/fish`) 응답의 어종 id를 사용합니다.

          ### 제약조건
          - `fishId` **필수** 쿼리 파라미터입니다.
          - 아직 잡지 않은 어종이어도 **에러가 아닙니다** → `200` + `catchCount:0` + 빈 목록(`imageUrls:[]`).

          ### ⚠ 예외상황
          - `401`: 토큰이 없거나 무효한 경우.
          - `400`: `fishId`가 누락되었거나 숫자가 아닌 경우(공통 파라미터 검증, `GlobalErrorCode`).
          - 존재하지 않는 `fishId`를 넘겨도 현재는 404가 아니라 빈 결과(`catchCount:0`)를 반환합니다.
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
                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "토큰 누락/무효",
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
  BaseResponse<CatchRecordResponse> getMyCatch(
      @Parameter(hidden = true) Long userId,
      @Parameter(description = "전체 도감 어종 id", example = "1") Long fishId);

  @Operation(
      summary = "내 도감 조회",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 전체 수집 대상 어종을 도감 순서(어종 ID 오름차순)대로 반환하며, 각 칸에 **로그인 사용자가** 잡았는지(`caught`)를 표시합니다.
          - `caught=true`면 도감 이미지를, `false`면 같은 이미지를 그림자(실루엣)로 렌더하도록 프론트가 분기합니다(그림자는 클라이언트 이펙트, 서버는 플래그만 내려줌).
          - `totalCount`(전체 수집 대상 수)와 `caughtCount`(내가 잡은 수)로 도감 완성도를 함께 계산할 수 있어, 별도 조회 없이 진행도 바를 그릴 수 있습니다. → docs/ranking.md

          ### 사용 방법
          - `GET /api/collections/dex` + 헤더 `Authorization: Bearer {accessToken}`
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).

          ### 제약조건
          - `fishes` 배열의 순서·집합은 `GET /api/fish` 전체 도감과 동일합니다(잡은 어종 여부만 덧입힘).

          ### rarity(희귀도) enum
          - `LOW` · `USUALLY` · `HIGH`

          ### ⚠ 예외상황
          - `401`: 토큰이 없거나 무효한 경우.
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
        responseCode = "401",
        description = "토큰 누락/무효",
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
  BaseResponse<MyDexResponse> getMyDex(@Parameter(hidden = true) Long userId);

  @Operation(
      summary = "사진으로 어종 분류 (Top-3 후보)",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 인증 사진 1장을 AI 모델 서버로 보내 **어종 후보 3개**를 신뢰도 순으로 반환합니다.
          - **아무것도 저장하지 않는 순수 조회입니다.** S3 업로드도, 도감 기록도 일어나지 않습니다.
            사용자가 후보 중 하나를 고른 뒤 `POST /api/collections/verify`를 호출해야 도감에 기록됩니다.
          - 분류와 저장을 나눈 이유: 모델 Top-1 정확도는 81%지만 **Top-3는 90.7%** 입니다.
            자동 확정하면 5건 중 1건꼴로 잘못된 어종이 도감에 박힙니다.

          ### 사용 방법
          - `POST /api/collections/classify` (`multipart/form-data`) + 헤더 `Authorization: Bearer {accessToken}`
          - FormData 키: `image` (이미지 파일 1개)

          ### 응답 처리 규칙 (중요)
          - `uncertain: true` 여도 **후보 3개는 그대로 표시합니다.** 재촬영 안내(`guide`)만 덧붙이세요.
            확신이 없다는 뜻이지 후보가 틀렸다는 뜻이 아닙니다.
          - `candidates`의 항목은 모두 선택 가능합니다(`fishId`가 항상 채워져 있음).
            그대로 `verify`의 `fishId`로 넘기면 됩니다.
          - `confidence`는 25클래스 softmax **원값**이라 후보들의 합이 1이 아닙니다.
            보정(temperature scaling) 전이라 과신 경향이 있으니 %로 노출할지는 신중히 결정하세요.
          - **모델은 24종만 압니다.** 향어·학꽁치 등은 후보에 정답이 아예 없습니다.
            → "목록에서 직접 선택" 대안 경로를 항상 함께 제공해야 합니다(`GET /api/collections/dex` 활용).
          - 모델 서버 장애(503)에도 인증 자체는 가능합니다. `verify`는 모델을 호출하지 않습니다.

          ### 제약조건
          - 이미지 파일만, 최대 5MB (분류 한도 = 저장 한도. 분류에 성공한 사진은 반드시 인증도 가능합니다)

          ### ⚠ 예외상황
          - `AI001(400)`: 사진이 비어 있음
          - `AI002(400)`: 이미지 파일이 아님
          - `AI003(400)`: 사진을 디코딩할 수 없음(손상 등)
          - `AI004(415)`: 지원하지 않는 이미지 형식
          - `AI005(413)`: 5MB 초과
          - `AI006(413)`: 해상도가 지나치게 큼
          - `AI007(503)`: 모델 미로드(서버 기동 중)
          - `AI008(503)`: 모델 서버 연결 불가 → 직접 선택 유도
          - `401`: 토큰 누락·무효
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "분류 성공",
        content =
            @Content(
                schema = @Schema(implementation = ClassifyResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "modelVersion": "b0-384-20260818",
                                "uncertain": false,
                                "guide": "후보 중에서 잡은 어종을 선택해주세요. 목록에 없으면 직접 선택할 수 있어요.",
                                "candidates": [
                                  { "rank": 1, "fishId": 15, "name": "붕어", "imageUrl": null, "confidence": 0.83 },
                                  { "rank": 2, "fishId": 16, "name": "잉어", "imageUrl": null, "confidence": 0.05 },
                                  { "rank": 3, "fishId": 20, "name": "가물치", "imageUrl": null, "confidence": 0.01 }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "사진이 비었거나 이미지가 아니거나 디코딩 실패",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "사진을 읽을 수 없습니다. 다른 사진으로 다시 시도해주세요.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "413",
        description = "사진 용량·해상도 초과",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 413, "message": "사진 크기는 5MB 이하여야 합니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "503",
        description = "모델 서버 연결 불가 → 목록에서 직접 선택 유도",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 503, "message": "어종 분류 서버에 연결할 수 없습니다. 목록에서 직접 선택해주세요.", "data": null }
                            """)))
  })
  BaseResponse<ClassifyResponse> classify(
      @Parameter(description = "인증 사진(이미지, 최대 5MB)") MultipartFile image);

  @Operation(
      summary = "어종 인증 (도감 기록)",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 사용자가 **확정한** 어종으로 인증 기록 1건을 저장합니다(인증 1건 = `catch_record` 1행).
          - 사진은 S3 `fish/` 경로에 업로드되고 URL이 기록에 저장됩니다.
          - **어종은 모델이 아니라 사용자가 정합니다.** `classify` 후보에서 골랐든, 24종 밖 어종이라
            도감 목록에서 직접 골랐든 동일하게 동작합니다. 따라서 **모델 서버가 죽어도 인증은 가능합니다.**
          - 응답의 `firstCatch`/`catchCount`는 저장된 컬럼이 아니라 (사용자, 어종) 행 집계에서 파생합니다.
            `firstCatch: true`면 도감 새 칸을 획득한 것이니 연출을 띄우기 좋습니다.

          ### 사용 방법
          - `POST /api/collections/verify` (`multipart/form-data`) + 헤더 `Authorization: Bearer {accessToken}`
          - FormData 키
            - `image`: 인증 사진(이미지 파일 1개)
            - `fishId`: 확정한 어종 id (`classify` 후보의 `fishId` 또는 `GET /api/collections/dex`의 어종 id)
            - `size`: 잡은 크기(cm, 실수 가능 — 예 `27.5`)

          ### 제약조건
          - 이미지 파일만, 최대 5MB
          - `size`는 필수이며 0 초과 300 이하(cm). 크기 랭킹(`GET /api/rankings/size`) 기준값이라 NOT NULL 입니다.
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).

          ### ⚠ 예외상황
          - `C001(400)`: 크기가 없거나 0 이하
          - `C002(400)`: 크기가 현실 범위(300cm) 초과
          - `F001(404)`: 해당 어종이 도감에 없음
          - `S001(400)`·`S002(400)`·`S003(400)`: 사진 없음·이미지 아님·5MB 초과
          - `S004(500)`: S3 업로드 실패
          - `401`: 토큰 누락·무효
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "인증 성공",
        content =
            @Content(
                schema = @Schema(implementation = VerifyResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "어종 인증이 완료되었습니다.",
                              "data": {
                                "catchRecordId": 42,
                                "fishId": 15,
                                "fishName": "붕어",
                                "imageUrl": "https://fishlog-bucket.s3.ap-northeast-2.amazonaws.com/fish/uuid.jpg",
                                "size": 27.5,
                                "firstCatch": true,
                                "catchCount": 1
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "크기 값이 올바르지 않거나 사진이 유효하지 않음",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 400, "message": "어종 크기(cm)는 0보다 커야 합니다.", "data": null }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "어종을 찾을 수 없음",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 404, "message": "해당 어종을 찾을 수 없습니다.", "data": null }
                            """)))
  })
  BaseResponse<VerifyResponse> verify(
      @Parameter(hidden = true) Long userId,
      @Parameter(description = "확정한 어종 ID", example = "15") Long fishId,
      @Parameter(description = "잡은 크기(cm)", example = "27.5") Double size,
      @Parameter(description = "인증 사진(이미지, 최대 5MB)") MultipartFile image);
}
