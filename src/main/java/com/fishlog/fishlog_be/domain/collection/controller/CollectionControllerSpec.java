package com.fishlog.fishlog_be.domain.collection.controller;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.ClassifyResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CustomCatchDetailResponse;
import com.fishlog.fishlog_be.domain.collection.dto.CustomCatchResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyCustomDexResponse;
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
          - 특정 어종에 대해 **로그인 사용자**의 최근 인증 사진(`recentCatches`)과 잡은 총 횟수(`catchCount`), 그리고 그 어종의 서식지(`habitat`)를 반환합니다.
          - 어종 상세 화면에서 "내가 이 물고기를 몇 번, 어떤 사진으로, 어디서 몇 cm 짜리를 잡았는지"를 보여줄 때 사용합니다.
          - `habitat`은 인증 기록이 아니라 **어종의 속성**이라, 아직 안 잡은 어종(`catchCount:0`)이어도 채워집니다.
            값 집합은 `바다` · `강` · `저수지` · `하천`이며, 콘텐츠가 없는 어종은 `null`일 수 있습니다.

          ### 화면 구성 가이드 (썸네일 4칸 + 오버레이)
          - `recentCatches`는 **최신순 최대 4장**입니다. 그대로 순서대로 썸네일에 깔면 됩니다.
            4장 미만이면 있는 만큼만 오고(0장이면 빈 배열), 별도 패딩은 없습니다.
          - 썸네일을 눌러 오버레이로 크게 띄울 때 필요한 값이 **항목마다 모두 들어 있습니다** —
            `imageUrl`(큰 사진) · `size`(그때 기록한 cm) · `location`(그때 수기 입력한 위치) · `verifiedAt`(등록 시각).
            추가 조회 없이 배열 항목 하나만 들고 오버레이를 그릴 수 있습니다.
          - `location`은 **선택 입력이라 `null`일 수 있습니다.** 오버레이에서 위치 줄을 숨기거나 "위치 미기록"으로 처리하세요.
          - `verifiedAt`은 촬영 시각이 아니라 **서버에 인증이 등록된 시각**입니다(EXIF를 읽지 않습니다).

          ### catchCount · maxSize vs recentCatches.length (중요)
          - `catchCount`(잡은 횟수)와 `maxSize`(최대 크기)는 **자르지 않은 전체 인증 기록 기준**이고,
            `recentCatches`만 4장으로 제한됩니다. 7번 잡았다면 `catchCount:7` + `recentCatches` 4개가 정상입니다.
          - 남은 장수는 `catchCount - recentCatches.length`로 계산해 "+3장 더" 같은 배지를 그릴 수 있습니다.
          - ⚠️ **`maxSize`를 `recentCatches`에서 계산하지 마세요.** 최대 크기가 잘려 나간 5번째 이후 기록에
            있을 수 있어, 응답에 온 4장의 최댓값과 다를 수 있습니다(서버 값이 맞습니다).
          - `maxSize`는 아직 안 잡은 어종이면 `null`입니다(`catchCount:0`).

          ### 사용 방법
          - `GET /api/collections?fishId={fishId}` + 헤더 `Authorization: Bearer {accessToken}`
            - 예: `GET /api/collections?fishId=1`
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).
          - `fishId`는 내 도감(`GET /api/collections/dex`) 응답의 어종 id를 사용합니다.

          ### 제약조건
          - `fishId` **필수** 쿼리 파라미터이며, 도감에 존재하는 어종이어야 합니다.
          - 아직 잡지 않은 어종이어도 **에러가 아닙니다** → `200` + `catchCount:0` + 빈 목록(`recentCatches:[]`) + `habitat`은 채워짐.
          - 5장 이상 잡았어도 사진은 항상 최대 4장만 옵니다(전체 사진을 받는 별도 엔드포인트는 아직 없습니다).

          ### ⚠ 예외상황
          - `401`: 토큰이 없거나 무효한 경우.
          - `400`: `fishId`가 누락되었거나 숫자가 아닌 경우(공통 파라미터 검증, `GlobalErrorCode`).
          - `F001(404)`: 도감에 없는 `fishId`인 경우. **"안 잡은 어종"과는 다릅니다** —
            어종 자체가 존재하면 잡지 않았어도 `200`입니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공(미인증 어종 포함). 사진은 최신순 최대 4장",
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
                              "habitat": "바다",
                              "catchCount": 7,
                              "maxSize": 38.2,
                              "recentCatches": [
                                {
                                  "catchRecordId": 42,
                                  "imageUrl": "https://.../fish/uuid1.jpg",
                                  "size": 31.0,
                                  "location": "격포항 방파제",
                                  "verifiedAt": "2026-09-01T14:32:10"
                                },
                                {
                                  "catchRecordId": 39,
                                  "imageUrl": "https://.../fish/uuid2.jpg",
                                  "size": 27.5,
                                  "location": null,
                                  "verifiedAt": "2026-08-24T08:11:02"
                                }
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
                            "data": { "habitat": "저수지", "catchCount": 0, "maxSize": null, "recentCatches": [] }
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
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "도감에 없는 어종(F001). 안 잡은 어종은 404가 아니라 200입니다.",
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
          - **잡은 횟수·인증 사진은 이 응답에 없습니다.** 그리드는 획득/미획득만 그리고, 칸을 눌렀을 때
            `GET /api/collections?fishId={id}`로 해당 어종의 `catchCount`·`imageUrls`를 조회하세요.
          - `totalCount`(전체 수집 대상 수)와 `caughtCount`(내가 잡은 수)로 도감 완성도를 함께 계산할 수 있어, 별도 조회 없이 진행도 바를 그릴 수 있습니다. → docs/ranking.md

          ### 사용 방법
          - `GET /api/collections/dex` + 헤더 `Authorization: Bearer {accessToken}`
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).

          ### 제약조건
          - `fishes` 배열의 순서·집합은 전체 도감과 동일합니다(잡은 어종 여부만 덧입힘).

          ### rarity(희귀도) enum
          - `LOW` · `USUALLY` · `HIGH`

          ### habitat(서식지)
          - 어종이 주로 서식하는 곳입니다: `바다` · `강` · `저수지` · `하천`
          - 도감을 서식지별 탭·그룹으로 묶을 때 사용합니다.
          - 콘텐츠가 아직 채워지지 않은 어종은 `null`일 수 있으니 "기타" 등으로 처리하세요.

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
                                    "habitat": "바다",
                                    "caught": true
                                  },
                                  {
                                    "id": 2,
                                    "name": "붕어",
                                    "imageUrl": "https://.../fish/2.png",
                                    "rarity": "LOW",
                                    "habitat": "저수지",
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
            - `location`: **잡은 위치(선택)** — 사용자가 직접 적는 자유 텍스트 (예: `충주호 종댕이길 선착장`)

          ### 잡은 위치(`location`) 입력 규칙
          - **선택 입력입니다.** 생략하거나 빈 값을 보내면 위치 없이(`null`) 기록됩니다.
          - 등록된 낚시 스팟을 고르는 것이 아니라 **사용자가 직접 적는 자유 텍스트**입니다.
            개인 포인트·유료 낚시터처럼 스팟 목록에 없는 장소도 그대로 기록할 수 있습니다.
          - 앞뒤 공백은 서버가 제거하고, 공백만 입력한 경우는 미입력(`null`)과 동일하게 처리합니다.
          - 최대 100자입니다.

          ### 제약조건
          - 이미지 파일만, 최대 5MB
          - `size`는 필수이며 0 초과 300 이하(cm). 크기 랭킹(`GET /api/rankings/size`) 기준값이라 NOT NULL 입니다.
          - `location`은 선택이며 최대 100자입니다.
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).

          ### ⚠ 예외상황
          - `C001(400)`: 크기가 없거나 0 이하
          - `C002(400)`: 크기가 현실 범위(300cm) 초과
          - `C003(400)`: 잡은 위치가 100자를 초과
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
                                "location": "충주호 종댕이길 선착장",
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
      @Parameter(description = "잡은 위치(수기 입력, 선택, 최대 100자)", example = "충주호 종댕이길 선착장")
          String location,
      @Parameter(description = "인증 사진(이미지, 최대 5MB)") MultipartFile image);

  @Operation(
      summary = "내 도감 외 어종 전체 조회(그리드)",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - `POST /api/collections/custom`으로 등록한 **도감 외 어종 전체**를 반환합니다. 파라미터는 없습니다(신원은 토큰).
          - **`GET /api/collections/dex`(내 도감 그리드)와 같은 자리의 API**입니다. 칸 하나 = 어종 하나이고,
            칸을 누르면 상세(`GET /api/collections/custom?customFishId=`)를 호출하는 흐름도 동일합니다.
          - **어종명이 같으면 같은 어종입니다.** 같은 이름으로 3번 등록했다면 칸은 **하나**이고 `catchCount`가 `3`입니다.

          ### 도감 그리드(`/dex`)와 다른 점
          - `caught` 없음 — 도감은 24칸 중 안 잡은 칸을 그림자로 그리지만, 이 목록은 **등록해야 생기는 칸**이라 전부 잡은 것입니다.
          - `rarity` 없음 — 희귀도는 도감 마스터 데이터의 속성이라 사용자가 만든 어종에는 없습니다.
          - `catchCount`·`maxSize` **있음** — 도감 그리드는 이 둘을 일부러 뺐지만, 이 목록은 칸에 바로 표시합니다.
          - `imageUrl`은 고정 도감 이미지가 아니라 **가장 최근에 등록한 사진**입니다(새 사진을 올리면 칸이 갱신됩니다).
          - 수 두 개의 의미가 다릅니다 → `totalCount`는 **내가 만든 어종 수**(= `fishes` 길이), `totalCatchCount`는
            **등록한 기록의 총 수**입니다. 도감처럼 "전체 몇 종"이라는 분모가 없어 완성도(%)를 계산하지 않습니다.

          ### 정렬
          - 어종 칸: **가장 최근에 잡은 어종부터**(그 어종의 최신 기록이 앞선 순).

          ### ⚠ 같은 어종 판정은 "문자열 완전일치"입니다
          - 등록 시 앞뒤 공백은 서버가 제거하므로 `"쏘가리 "`와 `"쏘가리"`는 같은 어종으로 모입니다.
          - 반면 `"우럭"`과 `"조피볼락"`은 같은 물고기라도 **다른 어종**이 됩니다. 입력 UI에서
            **이미 등록한 이름을 자동완성으로 제시**하면 표기가 갈리는 것을 크게 줄일 수 있습니다.

          ### 사용 방법
          - `GET /api/collections/custom/dex` + 헤더 `Authorization: Bearer {accessToken}`
          - 등록한 기록이 하나도 없어도 **에러가 아닙니다** → `200` + `totalCount:0` · `totalCatchCount:0` · `fishes:[]`.

          ### ⚠ 예외상황
          - `401`: 토큰이 없거나 무효한 경우.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공(등록 기록이 없으면 빈 목록)",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MyCustomDexResponse.class),
                examples = {
                  @ExampleObject(
                      name = "등록한 기타 어종이 있는 경우",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": {
                              "totalCount": 2,
                              "totalCatchCount": 4,
                              "fishes": [
                                {
                                  "id": 3,
                                  "name": "쏘가리",
                                  "imageUrl": "https://.../custom-fish/uuid1.jpg",
                                  "habitat": "강",
                                  "catchCount": 3,
                                  "maxSize": 41.0
                                },
                                {
                                  "id": 1,
                                  "name": "미꾸라지",
                                  "imageUrl": "https://.../custom-fish/uuid3.jpg",
                                  "habitat": null,
                                  "catchCount": 1,
                                  "maxSize": 12.5
                                }
                              ]
                            }
                          }
                          """),
                  @ExampleObject(
                      name = "등록한 기타 어종이 없는 경우",
                      value =
                          """
                          {
                            "success": true,
                            "code": 200,
                            "message": "요청이 성공적으로 처리되었습니다.",
                            "data": { "totalCount": 0, "totalCatchCount": 0, "fishes": [] }
                          }
                          """)
                })),
    @ApiResponse(
        responseCode = "401",
        description = "토큰 누락·무효",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 401, "message": "인증이 필요합니다.", "data": null }
                            """)))
  })
  BaseResponse<MyCustomDexResponse> getMyCustomDex(@Parameter(hidden = true) Long userId);

  @Operation(
      summary = "내 도감 외 어종 상세 조회",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 도감 외 어종 **한 종**에 대해, 내가 등록한 최근 사진(`recentCatches`)·총 횟수(`catchCount`)·최대 크기(`maxSize`)·서식지(`habitat`)를 반환합니다.
          - **`GET /api/collections?fishId=`(내 어종 인증 조회)와 같은 스펙**입니다. 어종 식별자만 `fishId` → `customFishId`로 바뀝니다.
            썸네일 4칸 + 오버레이 화면을 **같은 컴포넌트로 그릴 수 있습니다.**
          - `customFishId`는 전체 조회(`GET /api/collections/custom/dex`) 응답의 `fishes[].id`,
            또는 등록 응답의 `customFishId`를 사용합니다.

          ### 도감 상세와 다른 점
          - `name`(어종명)이 추가로 옵니다 — 도감은 클라이언트가 `fishId`로 어종을 이미 알지만,
            사용자가 만든 어종은 이름이 서버에만 있어 상세를 단독으로 열면 표시할 이름이 없기 때문입니다.
          - 사진 항목의 키 이름이 다릅니다: `catchRecordId`→**`customCatchRecordId`**, `verifiedAt`→**`registeredAt`**
            (가리키는 테이블이 다르고, 이 기록은 인증(verify)을 거치지 않았기 때문입니다).
          - `catchCount:0`인 상태가 없습니다 — 어종은 **등록해야 생기므로** 존재하면 기록이 1건 이상입니다.
            따라서 `maxSize`도 `null`이 되지 않습니다.

          ### catchCount · maxSize vs recentCatches.length (중요)
          - `catchCount`와 `maxSize`는 **자르지 않은 전체 기록 기준**이고, `recentCatches`만 최신순 4장으로 제한됩니다.
          - 남은 장수는 `catchCount - recentCatches.length`로 계산해 "+N장 더" 배지를 그릴 수 있습니다.
          - ⚠️ **`maxSize`를 `recentCatches`에서 계산하지 마세요.** 최대 크기가 잘려 나간 5번째 이후 기록에
            있을 수 있어, 응답에 온 4장의 최댓값과 다를 수 있습니다(서버 값이 맞습니다).

          ### 사용 방법
          - `GET /api/collections/custom?customFishId={customFishId}` + 헤더 `Authorization: Bearer {accessToken}`
            - 예: `GET /api/collections/custom?customFishId=3`

          ### ⚠ 예외상황
          - `401`: 토큰이 없거나 무효한 경우.
          - `400`: `customFishId`가 누락되었거나 숫자가 아닌 경우.
          - `C008(404)`: 그런 어종이 없거나 **다른 사용자의 어종**인 경우.
            남의 어종은 403이 아니라 404로 답합니다 — 어종의 존재 여부 자체를 알려주지 않기 위함입니다.
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공. 사진은 최신순 최대 4장",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomCatchDetailResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "customFishId": 3,
                                "name": "쏘가리",
                                "habitat": "강",
                                "catchCount": 3,
                                "maxSize": 41.0,
                                "recentCatches": [
                                  {
                                    "customCatchRecordId": 12,
                                    "imageUrl": "https://.../custom-fish/uuid1.jpg",
                                    "size": 41.0,
                                    "location": "한탄강 고석정",
                                    "registeredAt": "2026-09-05T14:32:10"
                                  },
                                  {
                                    "customCatchRecordId": 9,
                                    "imageUrl": "https://.../custom-fish/uuid2.jpg",
                                    "size": 34.0,
                                    "location": null,
                                    "registeredAt": "2026-08-30T07:15:44"
                                  }
                                ]
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "404",
        description = "그런 어종이 없거나 다른 사용자의 어종",
        content =
            @Content(
                examples =
                    @ExampleObject(
                        value =
                            """
                            { "success": false, "code": 404, "message": "등록한 도감 외 어종을 찾을 수 없습니다.", "data": null }
                            """)))
  })
  BaseResponse<CustomCatchDetailResponse> getMyCustomCatch(
      @Parameter(hidden = true) Long userId,
      @Parameter(description = "도감 외 어종 ID", example = "3") Long customFishId);

  @Operation(
      summary = "도감 외 어종 등록(수기 입력)",
      security = @SecurityRequirement(name = "JWT"),
      description =
          """
          ### 설명
          - 도감(24종)에 **없는 물고기**를 잡았을 때, 사진과 함께 **어종명 · 주요 서식지 · 크기 · 잡은 위치를 사용자가 직접 입력해** 기록으로 남깁니다.
          - AI 분류(`POST /api/collections/classify`)를 거치지 않습니다 — 모델도 도감 24종만 알기 때문에
            그 밖의 물고기에는 쓸 수 있는 후보를 주지 못합니다. 어종명은 전적으로 사용자가 정합니다.

          ### 도감 인증(`/verify`)과의 차이 (중요)
          - 저장되는 테이블이 다릅니다(`custom_catch_record`). 그래서 이 기록은
            **도감 그리드(`GET /api/collections/dex`)의 칸을 채우지 않고, 랭킹(`/api/rankings/*`)에도 반영되지 않습니다.**
            검증되지 않은 수기 어종명이 도감 완성도·크기 랭킹에 섞이지 않도록 한 의도적인 분리입니다.
          - 응답에 `firstCatch`·`catchCount`가 **없습니다.** 어종명이 자유 텍스트라 같은 물고기를
            "우럭"·"조피볼락"처럼 다르게 적으면 다른 어종으로 세어져, 신뢰할 수 없는 횟수가 되기 때문입니다.
          - 도감에 **이미 있는 어종명**을 보내면 저장하지 않고 `C006(400)`으로 막습니다.
            그대로 저장하면 도감 칸도 안 채워지고 랭킹에도 안 잡히는, 사용자가 이유를 알 수 없는 기록이 됩니다.
            이 경우 도감 인증(`POST /api/collections/verify`)을 사용하세요.

          ### 사용 방법
          - `POST /api/collections/custom` (`multipart/form-data`) + 헤더 `Authorization: Bearer {accessToken}`
          - FormData 키
            - `image`: 사진(이미지 파일 1개)
            - `fishName`: **어종명(필수)** — 사용자가 직접 적는 자유 텍스트 (예: `쏘가리`)
            - `habitat`: **주요 서식지(선택)** — 자유 텍스트 (예: `강`)
            - `size`: 잡은 크기(cm, 실수 가능 — 예 `34.0`)
            - `location`: **잡은 위치(선택)** — 자유 텍스트 (예: `한탄강 고석정`)
          - 사용자 신원은 토큰에서 얻습니다(userId 파라미터 없음).

          ### 입력 규칙
          - `fishName`: **필수**, 앞뒤 공백 제거 후 최대 30자. 공백만 보내면 미입력으로 거부됩니다(`C004`).
          - `habitat`: **선택**, 최대 20자. 생략하거나 공백만 보내면 `null`로 기록됩니다.
            - 값을 정해진 목록으로 **강제하지 않습니다.** 도감 어종은 시드가 `fishes.habitat`를 채우지만
              도감 밖 어종은 채워 줄 시드가 없어 잡은 사람이 직접 적는 값입니다.
            - 다만 도감이 쓰는 값은 `바다` · `강` · `저수지` · `하천` 네 가지이니, **입력 UI에서 이 값들을 후보로 제시**하고
              필요하면 직접 입력도 허용하는 형태를 권장합니다(나중에 서식지별로 묶어 보여줄 때 값이 갈리지 않습니다).
            - `location`(잡은 위치)과 다른 값입니다 — `location`은 "이번에 어디서 잡았나", `habitat`은 "이 물고기가 원래 어디 사나"입니다.
          - `size`: **필수**, 0 초과 300 이하(cm). 도감 인증과 동일한 한도입니다.
          - `location`: **선택**, 최대 100자. 생략하거나 공백만 보내면 위치 없이(`null`) 기록됩니다.

          ### ⚠ 예외상황
          - `C001(400)`: 크기가 없거나 0 이하
          - `C002(400)`: 크기가 현실 범위(300cm) 초과
          - `C003(400)`: 잡은 위치가 100자를 초과
          - `C004(400)`: 어종명이 비어 있음
          - `C005(400)`: 어종명이 30자를 초과
          - `C006(400)`: 이미 도감에 있는 어종명 → 도감 인증(`/verify`)을 사용
          - `C007(400)`: 주요 서식지가 20자를 초과
          - `S001(400)`·`S002(400)`·`S003(400)`: 사진 없음·이미지 아님·5MB 초과
          - `S004(500)`: S3 업로드 실패
          - `401`: 토큰 누락·무효
          """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "등록 성공",
        content =
            @Content(
                schema = @Schema(implementation = CustomCatchResponse.class),
                examples =
                    @ExampleObject(
                        value =
                            """
                            {
                              "success": true,
                              "code": 200,
                              "message": "도감 외 어종이 등록되었습니다.",
                              "data": {
                                "customCatchRecordId": 7,
                                "fishName": "쏘가리",
                                "habitat": "강",
                                "imageUrl": "https://fishlog-bucket.s3.ap-northeast-2.amazonaws.com/custom-fish/uuid.jpg",
                                "size": 34.0,
                                "location": "한탄강 고석정",
                                "registeredAt": "2026-09-05T14:32:10"
                              }
                            }
                            """))),
    @ApiResponse(
        responseCode = "400",
        description = "입력값이 올바르지 않거나, 이미 도감에 있는 어종명인 경우",
        content =
            @Content(
                examples = {
                  @ExampleObject(
                      name = "이미 도감에 있는 어종",
                      value =
                          """
                          { "success": false, "code": 400, "message": "이미 도감에 있는 어종입니다. 어종 인증으로 등록해주세요.", "data": null }
                          """),
                  @ExampleObject(
                      name = "어종명 미입력",
                      value =
                          """
                          { "success": false, "code": 400, "message": "어종명을 입력해주세요.", "data": null }
                          """)
                }))
  })
  BaseResponse<CustomCatchResponse> registerCustomCatch(
      @Parameter(hidden = true) Long userId,
      @Parameter(description = "사용자가 직접 입력한 어종명(필수, 최대 30자)", example = "쏘가리") String fishName,
      @Parameter(description = "주요 서식지(수기 입력, 선택, 최대 20자)", example = "강") String habitat,
      @Parameter(description = "잡은 크기(cm)", example = "34.0") Double size,
      @Parameter(description = "잡은 위치(수기 입력, 선택, 최대 100자)", example = "한탄강 고석정") String location,
      @Parameter(description = "사진(이미지, 최대 5MB)") MultipartFile image);
}
