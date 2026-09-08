# external.md — 외부 API 연동

> 외부 데이터/서비스 연동 계약(키·엔드포인트·응답·에러·캐싱 정책)을 기록합니다. API 키·시크릿은 환경변수(`be_config`)로 주입 → `docs/setup.md`.

## 1. 낚시 스팟 — 공공데이터포털 바다낚시지수 API 🚧 (스팟 시드 데이터 출처)

- **용도:** 낚시 스팟(Spot)의 **기준 데이터 출처**. 응답에서 **불변 정보만** 추출해 DB에 시드로 저장합니다 — 위치명·위도·경도(`spots`)와 스팟별 대상 어종(`major_fish`, 시점 불변). 낚시지수·날씨·물때는 저장하지 않고 실시간 호출 → `docs/geo.md`, `docs/spec.md`.
- **제공처:** 해양수산부 국립해양조사원 _ 바다낚시지수 조회 (공공데이터포털 데이터셋 **15142486**).
  - 문서 페이지: https://www.data.go.kr/data/15142486/openapi.do
  - **실제 호출 엔드포인트:** `https://apis.data.go.kr/1192136/fcstFishingv2/GetFcstFishingApiServicev2` (문서 페이지 URL이 아님에 유의)
- **인증키:** 환경변수 `DATA_GO_KR_SERVICE_KEY` (공공데이터포털 "일반 인증키(Decoding)" 권장). Decoding 키는 percent-encoding 필요, Encoding 키는 그대로 사용.
- **요청 파라미터**

  | 파라미터 | 값 | 설명 |
  |---|---|---|
  | `serviceKey` | 발급키 | 인증키 |
  | `type` | `json` | 응답 포맷(json/xml) |
  | `gubun` | `갯바위` / `선상` | 낚시 구분(필수). 두 구분 모두 조회해야 전체 위치가 수집됨 |
  | `pageNo` | 1.. | 페이지 번호 |
  | `numOfRows` | 최대 300 | 페이지당 개수 |

- **응답 구조(item 필드 발췌)** — 예보성(변동) 필드가 대부분이며, 저장에는 **불변 필드만** 사용합니다(위치명·위도·경도 + 대상 어종).

  | 필드 | 타입 | 의미 | 스팟 저장 |
  |---|---|---|---|
  | `seafsPstnNm` | string | 낚시터 위치명 | ✅ `name` |
  | `lat` | number | 위도 | ✅ `lat` |
  | `lot` | number | 경도(longitude) | ✅ `lot` |
  | `seafsTgfshNm` | string | 대상 어종명 | ✅ `major_fish`(정적 매핑, 스팟별 어종) |
  | `totalIndex` | string | 낚시지수(라벨) | ✖ 예보성 |
  | `predcYmd` / `predcNoonSeCd` | string | 예보 일자(`yyyy-MM-dd`)·오전/오후 구분(`오전`/`오후`, 먼 날은 `일`) | ✖ 예보성 |
  | `tdlvHrCn` | string | 물때(내용, 예: "중조기") | ✖ 예보성 |
  | `minWvhgt`/`maxWvhgt`/`minWtem`/`maxWtem`/`minArtmp`/`maxArtmp`/`minCrsp`/`maxCrsp`/`minWspd`/`maxWspd` | number | 파고·수온·기온·유속·풍속 | ✖ 예보성 |

  > ⚠️ 실제 v2 응답 확인(2026-08): `predcYmd`는 하이픈 포함 `yyyy-MM-dd`, `predcNoonSeCd`는 `오전`/`오후`(먼 날은 `일`). **낚시지수 점수(`lastScr`)·물때 점수(`tdlvHrScr`)는 문서 스키마엔 정수로 있으나 v2 실응답에서 값을 주지 않아(키 생략) 항상 null → 응답 매핑에서 제외**한다(낚시지수는 라벨 `totalIndex`, 물때는 `tdlvHrCn`만 제공).

- **수집 규모:** 전체 약 1,750건 × 2구분 = 3,500 레코드 → **고유 위치명(`seafsPstnNm`) 49개**. 이 49개가 곧 스팟 종류 수(추후 추가 가능). 대상 어종은 **7종**(감성돔·농어·돌돔·벵에돔·우럭·참돔 + `기타어종`), (스팟,어종) 페어 **160개**.
- **대상 어종은 시점 불변(실측):** 7일치 전량 비교 결과 `seafsTgfshNm`은 **오전/오후·날짜에 무관하게 스팟별 고정**(294개 (스팟,일자) 조합에서 오전 vs 오후 차이 0건). 따라서 예보가 아니라 **정적 매핑(`major_fish`)으로 저장**한다 → `docs/spec.md`.
- **어종 값 처리:** 플레이스홀더 `-`(대상어종 없음)는 시드에서 **제외**. catch-all `기타어종`은 확정 데이터셋에서 **실제 어종으로 대체**되어 더 이상 포함되지 않는다 → `docs/spec.md` 설계 결정 사항.
- **⚠️ 이 API는 더 이상 시드의 단독 출처가 아니다:** 스팟·어종은 **`data/spot/spot_master.json`(확정 데이터셋, 스팟 99행 → 적재 92곳·어종 24종)** 으로 확정되었고, 이 API의 지점실측 6종은 그 일부(바다 스팟의 `source="지점실측"`)로 편입됐다. 나머지는 국립생태원 담수 실측·해역 어획통계에서 온다. → `docs/spec.md` "스팟·어종 확정 데이터셋".
- **시드 생성 스크립트:** `data/spot/build_seed.py` — `spot_master.json`을 읽어 두 시드를 생성한다(API 호출 없음). 결과:
  - `data/spot/spots_seed.json` — 스팟 `name`/`lat`/`lot`/`category` (spots 시드, 실측 상세 없는 담수 6곳 제외 + 이름 중복 분리/병합 후 92곳). `category`("바다"/"담수")는 `Spot.category`(해양/내륙)로 적재 → `docs/spec.md`.
  - `data/spot/spot_fish_seed.json` — 어종 24종 + (스팟, 어종) 페어 552개 (major_fish 시드)
  - `data/spot/inland_detail_seed.json` — 내륙 스팟 43곳의 하폭·유수폭·수심(단위 m, inland_spot_detail 시드) → `docs/spec.md` "담수 스팟 상세 시드"
  - (참고) `spot.py`(위치명 집계)·`fishDex.py`(어종명 전역 집계)·`seed.py`(구 API 수집기)는 **탐색·이력용**. 현재 시드 생성 경로는 `build_seed.py` 하나다.
  - 로더: 생성된 시드 JSON은 `global/init`의 `SeedDataReader`/`SeedDataInitializer`가 읽어 `SpotSeedLoader`(upsert)로 적재한다. → `docs/spec.md`.
- **연동 방식(불변 정보):** 실시간 호출이 아니라 **사전 수집(배치) 후 시드 적재**. 스팟 좌표(`spots`)·대상 어종 카탈로그(`major_fish`)는 불변이므로 초기 1회(또는 스팟 추가·주기 재수집 시) 수집 → DB seed.
- **연동 방식(예보성 정보) ✅ 구현됨:** 낚시지수·날씨·물때는 저장하지 않고 스팟 상세 조회(`GET /api/spots/{id}`) 시 실시간 호출한다. 서버 런타임 클라이언트 계층 **`global/forecast`**(`FishingIndexClient`=API 호출·파싱, `ForecastService`=캐시·필터)를 신설했다. 매 요청 원본 호출(전체 1,750건)은 지연·쿼터 위험이라, 두 구분(갯바위·선상) 전체 예보를 받아 **스팟명→예보목록 맵으로 단일 Redis 키에 12h(`RedisConfig.FORECAST_TTL`) 캐시**하고 `seafsPstnNm`으로 필터해 서빙한다. RestClient 타임아웃 3s(`RestClientConfig`), 외부 실패·타임아웃 시 예외를 전파하지 않고 `forecast=null`로 폴백(상세 base 정보는 항상 200) → `docs/spec.md` "스팟 데이터 설계".

> ⚠️ 오프셋 지명 주의: 위치명 중 일부는 기준점 기준 오프셋 표기(예: `강릉항 북동(2km)`, `목포북항 서측(53km)`)입니다. 좌표는 API 응답 `lat`/`lot`을 그대로 신뢰하며, 지명 문자열을 지오코딩하지 않습니다.

## 2. 어종 분류 AI — 자체 모델 서버 ✅ (EC2, FastAPI)

사진으로 어종을 분류하는 **자체 모델 서버**. 별도 레포에서 학습·배포하며, 이 백엔드는 호출만 한다.

```
[앱] 사진 촬영 → [백엔드 EC2] → [모델 EC2 ${MODEL_EC2_HOST}:8000] → [백엔드] → [앱]
```

- **모델 서버는 상태를 갖지 않는다.** 사용자·인증·도감 매핑은 전부 백엔드 책임이다.
- ⚠️ **모델 서버에는 인증이 없다.** 보안그룹으로 백엔드 EC2에서만 접근 가능하게 막혀 있으므로, **이 주소를 외부에 노출하거나 클라이언트가 직접 호출하게 만들면 안 된다.** 그래서 `POST /api/collections/classify`도 보호(로그인 필요) 엔드포인트다.
- 📍 **실제 주소는 이 문서에 적지 않는다.** 이 저장소는 public이므로, `${MODEL_EC2_HOST}`의 실값은 private 설정 서브모듈(`be_config`)의 `external.fish-classify.base-url` 한 곳에만 둔다.
- 로컬(`local` 프로파일)에서는 사설 IP에 도달할 수 없다 → connect timeout 1초 뒤 `AI008(503)` fallback으로 떨어진다. 정상 동작이다.

### 구현 위치

| 클래스 | 역할 |
|---|---|
| `global/ai/FishClassifyClient`(+`Impl`) | `POST /predict` multipart 호출·재시도·에러 매핑 |
| `global/ai/dto/PredictResponse`·`PredictionItem` | 모델 응답(성공/실패 공용) |
| `global/ai/AiErrorCode` | `AI001~AI008` — 모델 `error` 코드 ↔ 우리 에러 코드 매핑 |
| `global/config/RestClientConfig#fishClassifyRestClient` | connect 1s / read 5s 타임아웃 **재사용 빈** |

### 계약

**`POST /predict`** — `multipart/form-data`, 필드명 **`file`**

```jsonc
// 성공
{
  "success": true, "uncertain": false, "model_version": "b0-384-20260818",
  "predictions": [
    {"rank": 1, "species": "붕어",   "confidence": 0.83},
    {"rank": 2, "species": "잉어",   "confidence": 0.05},
    {"rank": 3, "species": "가물치", "confidence": 0.01}
  ],
  "other_confidence": 0.01, "top1_confidence": 0.83, "latency_ms": 81.2
}
// 실패
{"success": false, "error": "<코드>", "detail": "..."}
```

| 상황 | 모델 HTTP | 모델 `error` | → 우리 코드 |
|---|---|---|---|
| 빈 파일 | 400 | `EMPTY_FILE` | `AI001(400)` |
| (이미지 아님 — 백엔드 선검증) | — | — | `AI002(400)` |
| 손상·비이미지 | 400 | `IMAGE_DECODE_FAILED` | `AI003(400)` |
| 미지원 포맷 | 415 | `UNSUPPORTED_FORMAT` | `AI004(415)` |
| 용량 초과 | 413 | `FILE_TOO_LARGE` | `AI005(413)` |
| 화소 5천만 초과 | 413 | `IMAGE_TOO_LARGE` | `AI006(413)` |
| 모델 미로드 | 503 | `MODEL_NOT_LOADED` | `AI007(503)` |
| 연결 불가·타임아웃·모르는 코드 | — | — | `AI008(503)` |

그 외: `GET /health` → `{"status":"ok","model_version":"...","num_classes":25,...}` (미로드 시 503), `GET /labels` → 25종 목록과 학명·서식지.

### 반드시 지킬 것 ⚠️

1. **원본 바이트 그대로 전달한다 — 리사이즈·재인코딩 금지.** 모델 서버는 학습과 비트 단위로 같은 전처리를 하도록 맞춰져 있어서, 앞단에서 JPEG를 다시 구우면 **정확도가 조용히 떨어진다**. 크기 제한이 필요하면 리사이즈가 아니라 **거부**로 처리한다(`AI005`).
2. **`RestClient`는 빈으로 만들어 재사용한다.** 요청마다 새로 만들면 처리량이 **15건/초 → 3.4건/초**로 떨어지는 것이 실측됐다.

### 재시도 정책 ✅

- **4xx는 재시도하지 않는다.** 입력이 잘못된 것이라 다시 보내도 같은 답이다 → 모델의 `error`를 `AiErrorCode`로 옮겨 사용자에게 이유를 알린다.
- **5xx·타임아웃·네트워크 오류만 1회 재시도**한다(모델 평균 응답 80ms, read timeout 5s라 재시도 비용이 낮다). 최종 실패는 예외가 아니라 `Optional.empty()`로 돌려 호출부가 "직접 선택" 대안 경로로 안내하게 한다.

### 종명 = 두 시스템의 조인 키 ✅ (대조 완료)

모델이 주는 `species` 문자열과 `fishes.name`이 **정확히 일치**해야 도감 매핑이 성공한다. 표기 차이(우럭/조피볼락, 광어/넙치, 배스/큰입배스)가 있으면 조용히 실패한다.

- **대조 결과(현재): 모델 24종 ↔ `data/fish/fish_content_seed.json` 24종 — 문자열까지 완전 일치, 불일치 0건.**
- 모델 24종: 감성돔, 농어, 돌돔, 벵에돔, 우럭, 참돔, 광어, 볼락, 갈치, 고등어, 삼치, 방어, 전갱이, 숭어, 붕어, 잉어, 쏘가리, 배스, 블루길, 가물치, 메기, 송어, 피라미, 동자개
- 매핑 실패 시 그 후보는 **WARN 로그와 함께 제외**된다(선택 불가능한 후보를 내려보내지 않기 위해). **모델을 재학습해 클래스가 바뀌면 이 표와 시드를 함께 갱신할 것.**

### 정확도와 UX 제약 ⚠️

- Top-3 **90.7%** / Top-1 **81%**. 사용자가 Top-3에서 고르는 구조라 이 수치로 충분하다. → **Top-1 자동 확정 금지**(`docs/spec.md` 참고).
- **24종 밖 어종(향어·학꽁치 등)은 Top-3에 정답이 아예 없다.** "목록에서 직접 선택" 대안 경로가 **반드시** 필요하다.
- `confidence`는 보정 전 원값이라 과신 경향이 있다. 서버는 원값 + `rank`를 그대로 내려주고 표시 방식은 클라이언트가 정한다.

### 연동 확인

```bash
MODEL_EC2_HOST=$(grep external.fish-classify.base-url src/main/resources/application-prod.properties | sed "s|.*//||")
curl -s http://${MODEL_EC2_HOST}/health
curl -s -F "file=@fish.jpg" http://${MODEL_EC2_HOST}/predict
```

`/health`가 200이면 통신은 끝이다. 깨진 파일을 보내도 **4xx + JSON**이 오면 정상이다(500이 오면 안 된다).

### 미결정 항목 📋

- [ ] 24종 밖 어종을 잡았을 때의 앱 UX("직접 선택" 화면 소유 주체)
- [ ] `confidence`를 사용자에게 %로 노출할지 — 노출한다면 temperature scaling 보정이 먼저다
- [ ] 사용자 확정 결과 로깅(모델 Top-1 ≠ 사용자 확정 사례가 재학습에 가장 값지다). 수집 동의·개인정보 처리방침 필요

## 2. 관광 정보 — TourAPI (한국관광공사 KorService2) ✅

- **용도:** 사용자 **현재 위치 주변 관광 장소**(관광지·숙박·음식점) 조회 (`docs/product.md`의 Tour 도메인).
- **제공처:** 한국관광공사_국문 관광정보 서비스 (공공데이터포털 데이터셋 **15101578**, TourAPI 4.0 = `KorService2`).
  - 문서 페이지: https://www.data.go.kr/data/15101578/openapi.do
  - **실제 호출 엔드포인트:** `https://apis.data.go.kr/B551011/KorService2/locationBasedList2`
- **인증키:** data.go.kr **동일 계정키를 재사용**한다(§1 바다낚시지수와 같은 키). 프로퍼티 `external.tour.service-key`가 `external.fishing-index.service-key`(로컬)·`${API_DECODING_KEY}`(prod)를 가리킨다. ⚠️ **데이터셋 15101578 활용신청·승인이 별도로 필요**하다(미승인 시 인증 오류).

### 오퍼레이션 · 카테고리 매핑

`locationBasedList2`(위치기반 관광정보 목록)를 사용한다. 카테고리는 `contentTypeId`로 지정한다.

| 요청 `type`(한글) | `contentTypeId` |
|---|---|
| 관광지 | 12 |
| 숙박 | 32 |
| 음식점 | 39 |

**요청 파라미터**

| 파라미터 | 값 | 설명 |
|---|---|---|
| `serviceKey` | 발급키 | 인증키(Decoding 키는 percent-encoding) |
| `MobileOS` | `ETC` | 필수 |
| `MobileApp` | `fishlog` | 필수(앱명, `external.tour.mobile-app`) |
| `_type` | `json` | 응답 포맷(생략 시 XML) |
| `arrange` | `E` | 정렬: **거리순** |
| `mapX` / `mapY` | 경도 / 위도 | 검색 중심(우리 요청 `lng`/`lat`) |
| `radius` | m (기본 5000, 최대 20000) | 검색 반경 |
| `contentTypeId` | 12/32/39 | 카테고리 |
| `numOfRows` | 30 | 페이지당 개수(고정) |
| `pageNo` | 1.. | 페이지 |

### 응답 매핑 (우리 서비스가 쓰는 필드)

`response.body.items.item[]`에서 **7필드만** 추출한다.

| 필드 | 의미 | 응답 |
|---|---|---|
| `title` | 장소명 | ✅ `title` |
| `firstimage`/`firstimage2` | 대표/썸네일 이미지 | ✅ `firstImage`/`firstImage2`(빈 문자열 → null) |
| `addr1`/`addr2` | 기본/상세 주소 | ✅ `addr1`/`addr2`(빈 문자열 → null) |
| `mapx`/`mapy` | 경도/위도 | ✅ `mapX`/`mapY`(String → Double) |

### 연동 방식 ✅ — 매 요청 실시간 프록시(캐시·DB 없음)

- **Redis·DB를 쓰지 않고 매 요청 TourAPI를 실시간 호출**한다(제품 제약). 앵커가 고정 스팟이 아니라 **사용자 현재 GPS 좌표**라 좌표별 캐시 효율이 낮고, "항상 최신"이 요구사항이다.
- 서버 런타임 클라이언트: **`global/tour`** — `TourApiClient`(+`Impl`, 호출·파싱·검증), `dto/TourApiItem`·`TourApiResult`, `TourErrorCode`(T001~T003). 기능 도메인은 **`domain/tour`**(컨트롤러·서비스·`TourCategory`·응답 DTO). RestClient 타임아웃 connect 2s/read 5s(`RestClientConfig#tourApiRestClient`).
- **재시도:** 입력 문제(4xx)는 재시도하지 않고 `TOUR_API_ERROR(502)`. 연결 실패·타임아웃만 1회 재시도 후 실패 시 `TOUR_API_UNAVAILABLE(503)`.
- **⚠️ 쿼터:** 실시간·무캐시라 사용자 요청 1건 = 외부 1콜(재시도 시 최대 2콜). data.go.kr 일일 쿼터를 소진하기 쉬우므로 **운영 계정 쿼터 상향**이 사실상 전제다.

### data.go.kr 응답 방어 ⚠️

- `items.item`은 **단건이면 객체 / 다건이면 배열**, 결과가 없으면 `items`가 **빈 문자열**이다 → 레코드 바인딩 대신 `JsonNode`로 관대하게 파싱한다(`FishingIndexClientImpl`과 동일).
- `header.resultCode` 확인(성공 `"0000"`). 쿼터 초과·비정상 응답 시 XML/HTML 이 올 수 있어 JSON 파싱 실패도 `TOUR_API_ERROR`로 처리한다.

### 미결정 항목 📋

- [ ] 데이터셋 15101578 활용신청 승인 상태 확인, 운영 쿼터 규모 결정
- [ ] "이미지 있는 장소 우선/필터" 정책(현재는 거리순 그대로, 이미지 유무 무관)

## 3. 날씨 / 물때 / 조위
- 용도: 스팟 상세에서 낚시 조건(날씨·물때·조위) 제공.
- ✅ **§1 바다낚시지수 API로 구현됨** — 파고·수온·기온·유속·풍속·물때(`tdlvHrCn`)를 해양 스팟 상세(`GET /api/spots/{id}`)의 `forecast`로 제공한다(연동 방식은 §1 참고).
- 조위표 등 더 상세한 물때/조위 데이터가 필요해지면 별도 제공처(기상청·바다타임 등) 도입 검토 📋.

## 4. 지도 (Kakao / Naver Map)
- 용도: 지도 표시, 지오코딩/역지오코딩, 주변 검색 보조 (`docs/geo.md`와 연동).
- 확정 필요: 제공처(Kakao vs Naver), 서버 사이드 사용 범위(대부분 지도 표시는 클라이언트), 서버가 필요한 API(지오코딩 등).

## 공통 규칙 (권장)
- 외부 호출은 별도 client/adapter 계층으로 감싸고, 타임아웃·재시도·실패 시 폴백 정의.
- Rate limit·쿼터를 고려해 가능하면 결과를 캐시(로컬 DB/캐시)하고 주기적으로 갱신.
- 키/시크릿은 코드에 하드코딩 금지 — 환경변수(`be_config`)로만 주입.

## 확정 필요 항목 (공통)
- [ ] 각 API 제공처 계정·키 발급 주체
- [ ] 실시간 호출 vs 사전 수집(배치) 정책
- [ ] 응답 스키마 → 내부 도메인 모델 매핑
