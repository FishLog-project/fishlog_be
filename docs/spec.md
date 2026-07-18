# spec.md — API 엔드포인트·모델(ERD)

> API 명세와 DB 모델의 단일 출처. 코드 추가/변경 시 **이 문서를 함께 갱신**합니다 (`docs/conventions.md`). 현재는 스켈레톤 + 초안입니다.

## API 엔드포인트

모든 API는 `/api` 베이스 경로. 응답은 공통 응답 포맷(`docs/architecture.md`) 확정 후 반영.

| 상태 | Method | Path | 설명 | 인증 |
|---|---|---|---|---|
| 📋 | GET | `/api/health` | 헬스 체크 (미구현 — 배포 헬스체크·모니터링 필요 시 도입) | 공개 |
| 📋 | POST | `/api/auth/signup` | 회원가입 | 공개 |
| 📋 | POST | `/api/auth/login` | 로그인(JWT 발급) | 공개 |
| 📋 | GET | `/api/spots` | 낚시 스팟 목록/주변 검색 (DB 불변 정보만) | 공개 |
| 📋 | GET | `/api/spots/{id}` | 스팟 상세 = DB 기본정보 + **실시간 예보(낚시지수·날씨·물때·대상 어종)** 병합 | 공개 |
| ✅ | GET | `/api/fish` | 전체 도감 목록(수집 대상 어종 + 총 수). `?name=`으로 이름 완전일치 검색 | 공개 |
| ✅ | GET | `/api/fish/{id}` | 어종 상세 | 공개 |
| ✅ | GET | `/api/collections` | 특정 어종의 내 인증 요약(잡은 횟수 + 인증 사진 URL 목록). `userId`(임시)·`fishId` 파라미터 | 공개(임시) |
| 📋 | POST | `/api/collections/verify` | 어종 사진 인증 업로드(S3) | 보호 |
| 📋 | GET | `/api/collections/me` | 내 어종 도감 전체 조회 | 보호 |

> 위 경로는 초안입니다. 도메인 확정 시 Request/Response 스키마와 함께 상세화.

## Request / Response 스키마

모든 응답은 공통 래퍼 `BaseResponse<T>`(`success`/`code`/`message`/`data`)로 감싼다 → `docs/architecture.md`.

### 전체 도감 (어종 카탈로그) ✅

**`GET /api/fish`** — 전체 도감 목록 / 이름 검색. 공개. 페이징 없음, `id` 오름차순. `is_collectible=true` 인 수집 대상 어종만 반환.

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | String | 선택 | 어종명 **완전일치** 검색. 있으면 일치 어종만(0~1건), 없거나 공백이면 전체 목록. |

- `name` 검색 시에도 `is_collectible=false`(예: `기타어종`)는 조회되지 않는다.
- 일치하는 어종이 없으면 **404가 아니라 `200 + 빈 목록`**(`totalCount:0`). 컬렉션 필터이므로 "조건에 맞는 것 없음"은 정상 응답이다(단건 지목인 `GET /api/fish/{id}`의 404와 대비).

```json
{
  "success": true,
  "code": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "totalCount": 6,
    "fishes": [
      { "id": 1, "name": "감성돔", "imageUrl": null, "rarity": "USUALLY" }
    ]
  }
}
```

`GET /api/fish?name=감성돔` → `totalCount:1` + 감성돔 1건. `GET /api/fish?name=없는어종` → `totalCount:0` + 빈 목록.

**`GET /api/fish/{id}`** — 어종 상세. 공개. `is_collectible=true` 인 어종만 조회되며, 없거나 비수집 종이면 404(`F001` 해당 어종을 찾을 수 없습니다.).

```json
{
  "success": true,
  "code": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "id": 1,
    "name": "감성돔",
    "description": "은빛 몸에 검은 지느러미를 두른 갯바위 낚시의 대표 어종. 경계심이 강해 낚기 까다롭다.",
    "habitat": "바다",
    "imageUrl": null,
    "rarity": "USUALLY"
  }
}
```

> `description`·`habitat`은 콘텐츠 시드(`data/fish/fish_content_seed.json`)로 채워진다 → 아래 "어종 도감 콘텐츠 시드". `imageUrl`·`rarity`는 아직 큐레이션 전이라 `null`로 응답된다.

### 그 외 엔드포인트
📋 TBD — 나머지 엔드포인트별 요청/응답 예시(JSON)와 유효성 규칙을 여기에 기록.

## 스팟 데이터 설계 — 저장(불변) vs 실시간(예보) 🚧

스팟 정보를 성격에 따라 **DB 저장**과 **요청 시 실시간 호출**로 분리합니다. (바다낚시지수 API 15142486 → `docs/external.md` §1)

| 성격 | 대상 | 처리 |
|---|---|---|
| **불변** | 위치명·위도·경도(그리고 서비스 운영값 `prohibit`) | DB에 시드 저장(`spots`). 목록/지도 마커·주변 검색에 사용 |
| **정적 매핑** | 스팟에서 잡히는 대상 어종(`seafsTgfshNm`) | DB에 시드 저장(`major_fish`, `fishes` 연동). 배치로 스팟별 어종 수집·고유화 |
| **예보성(가변)** | 낚시지수(`totalIndex`/`lastScr`)·날씨(파고·수온·기온·유속·풍속)·물때(`tdlvHrScr`/`tdlvHrCn`) | **저장하지 않음.** 스팟 **상세 조회 시점**에 외부 API를 호출·파싱해 응답에 병합 |

**흐름:** `GET /api/spots/{id}` → ① DB에서 스팟 기본정보 + 대상 어종(`major_fish`) 조회 → ② 외부 API 예보(Redis 캐시)에서 해당 스팟의 낚시지수·날씨·물때 파싱 → ③ 병합 응답.

**설계 결정 사항**
- **대상 어종 = 정적 매핑 단일화 ✅(확정):** 대상 어종(`seafsTgfshNm`)은 **오전/오후·날짜에 무관하게 고정**임을 실측으로 확인(7일치 294개 (스팟,일자) 조합에서 오전 vs 오후 차이 0건, 스팟별 어종 집합 불변). 따라서 예보가 아니라 **스팟의 정적 속성**으로 취급하여 **`major_fish`에 저장하는 한 갈래로만** 처리한다. (실시간 파싱으로 어종을 뽑는 방식은 폐기.)
  - `major_fish`에 배치로 **(스팟, 어종) 페어**를 수집·고유화하고 `fishes.name`에 매핑. 스팟 상세의 "주요 대상 어종" 목록·도감(`catch_record`)/완성도 기준.
  - **수집 결과(현재):** 고유 스팟 **49개**, (스팟,어종) 페어 **160개**. 어종은 API가 제공하는 **7종**(감성돔·농어·돌돔·벵에돔·우럭·참돔 + `기타어종`). 수집기 `data/spot/seed.py`가 두 시드(`spots_seed.json`·`spot_fish_seed.json`)를 생성 → `docs/external.md` §1.
  - 어종명→`fishes` 매핑 규칙, `season`(어종 시즌)은 API에 없어 **TBD**.
  - 단, 위 실측은 7일 스냅샷 기준이라 **계절 단위 변동 가능성**은 열려 있음 → 주기적(예: 월 1회) 재수집으로 `major_fish` 갱신 권장.
- **`기타어종` 처리 ✅(확정):** API의 catch-all 카테고리 `기타어종`(현재 34개 페어)은 특정 어종이 아니라 도감 항목으로 부적절하다. `fishes`/`major_fish`에는 **행을 그대로 두되**(스팟-어종 매핑에 필요), `fishes.is_collectible=false`로 저장해 **전체 도감 조회(`GET /api/fish`)에서는 제외**한다. 시드의 제외 집합은 `SpotSeedLoader.NON_COLLECTIBLE_FISH_NAMES`(`기타어종`, `-`)로 관리한다.
  - **후속 계획 📋 TBD:** `기타어종`이 실제로 어떤 어종들을 포괄하는지 **별도 조사** 후, 그 결과로 **도감(`fishes`)을 더 풍부하게 채워 넣을** 예정. (바다낚시지수 API가 제공하는 어종은 6종뿐이라 도감 콘텐츠로는 부족 → 어종 마스터 카탈로그는 이 API와 분리해 확장하는 방향, 상세 미확정.)
  - **플레이스홀더 `-` 제외 ✅:** 대상어종 없음(`-`)은 실어종이 아니므로 `major_fish` 시드에서 제외한다(수집기에서 필터).
- **대상 어종 없는 스팟 = 빈 값 허용 ✅(확정):** 선상 오프셋 지명 스팟(예: `안흥항서측(40km)`) 등 **15개 스팟**은 API에 특정 대상어종이 없다. 이 스팟은 `major_fish` 매핑이 **0건**이어도 무방하며, 상세 응답의 "주요 대상 어종"은 **빈 값(정보 없음)** 으로 처리한다.
- **호출 효율/캐싱 ✅:** 예보(낚시지수·날씨·물때)는 API가 스팟 단건 필터 없이 `gubun`별 전체(약 1,750건)를 페이지네이션으로 반환 → 상세 요청마다 원본 호출은 지연·쿼터 위험. **Redis 캐시, 반나절 TTL로 확정**(예보 주기가 `predcYmd`+`predcNoonSeCd`로 굵음). 전체 예보를 캐시하고 상세는 `seafsPstnNm`으로 필터해 서빙.
- **실패 격리 📋 TBD:** 예보 외부 호출이 상세 응답 경로에 있음 → 타임아웃·재시도·폴백(DB 기본정보+대상 어종은 항상 응답, 예보 블록만 `null`+안내) 정책은 **TBD**. → `docs/external.md` 공통 규칙과 함께 확정.
- **시드 적재 전략(환경별) 🚧:**
  - **로컬 ✅:** `data/spot/seed.py` 산출 JSON을 `global/init`의 `SeedDataInitializer`(@PostConstruct)+`SpotSeedLoader`가 적재. `fishlog.seed.enabled=true`일 때만 동작하고, 이미 적재됐으면(`spots.count()>0`) 건너뜀(idempotent). 이어서 `FishContentSeedLoader`가 어종 콘텐츠를 채운다(아래).
  - **운영(prod) = Flyway 마이그레이션 도입 결정, 구현 📋 TBD:** 운영 시드/레퍼런스 데이터는 **Flyway(버전드 SQL 마이그레이션)로 적재·갱신**한다.
    - **근거:** 어종 카탈로그(`fishes`)를 API 제공 6종 외에 **수동 큐레이션으로 +20~30종 점진 확장**할 예정이라, "비었을 때 1회 적재"(JSON+count 가드)로는 증분 갱신이 안 됨 → 버전드 증분·이력·재현성이 필요.
    - **TBD 항목:** `flyway`(+`flyway-mysql`) 의존성 추가, 스키마 관리 이관(prod `ddl-auto=validate`/`none` 전환, 로컬 정책), 초기 시드(JSON→`V__init_*.sql`) 및 큐레이션 배치(`V__add_fishes_*.sql`) 생성·버전 관리 절차, 로컬 부트스트랩을 JSON 로더 유지 vs Flyway 통일.
    - 어종 콘텐츠 시드(아래)도 로컬 전용이므로 이 이관 대상에 포함된다.

## 어종 도감 콘텐츠 시드 ✅

어종의 `description`·`habitat`을 **로컬 기동 시 자동 적재**한다. 데이터는 `data/fish/fish_content_seed.json`, 적재는 `global/init/FishContentSeedLoader`.

| 항목 | 내용 |
|---|---|
| 시드 파일 | `data/fish/fish_content_seed.json` (프로젝트 루트 `data/`, 서브모듈 아님) |
| 경로 프로퍼티 | `fishlog.seed.fish-content-location` (기본 `file:data/fish/fish_content_seed.json`) |
| 스키마 | `{ "fishes": [ { "name", "habitat", "description" } ] }` — `name`이 `fishes.name`(UNIQUE)과 매칭되는 키 |
| 대상 | 수집 대상 6종(감성돔·농어·돌돔·벵에돔·우럭·참돔). `기타어종`·`-`는 시드에 없음 |
| 실행 시점 | `SeedDataInitializer`가 **스팟 시드 다음에** 호출 (어종 행이 먼저 존재해야 하므로) |
| 활성 조건 | `fishlog.seed.enabled=true` (= 로컬 전용, 운영은 Flyway 트랙) |

**설계 결정 사항**
- **`habitat`은 전 어종 `"바다"` ✅(확정):** 대상 6종이 모두 해수어라 현재는 단일 값. 민물 어종을 도감에 추가하면 이 컬럼이 의미를 갖는다.
- **`SpotSeedLoader`와 분리 ✅(확정):** 스팟 시드는 `spots.count()>0` 이면 통째로 건너뛰는 가드가 있고 어종도 **없을 때만 insert** 한다. 콘텐츠를 거기에 얹으면 **이미 적재된 DB에는 영영 반영되지 않는다.** 따라서 콘텐츠 로더는 가드와 무관하게 매 기동 실행되며 **기존 행을 update** 한다(`Fish.applyContent()` + JPA dirty checking, `save()` 호출 없음).
- **적용 정책 = 항상 덮어쓰기 ✅(확정):** JSON이 도감 콘텐츠의 **단일 진실 공급원**. 기동 때마다 시드 값으로 덮어쓰므로 JSON 수정 → 재시작만으로 반영된다. 값이 같으면 Hibernate가 UPDATE를 생략하므로 반복 비용은 없다.
  - **트레이드오프:** DB에서 직접 수정한 콘텐츠는 **다음 기동에 사라진다.** 관리자 편집 기능을 도입하면 이 정책을 재검토해야 한다 📋.
- **미해결 이름은 스킵 ✅:** 시드에 있으나 DB에 없는 어종명은 `WARN` 로그 후 건너뛴다(예외 아님).

## 사용자 도감 (어종 인증) — `catch_record` ✅

전체 도감(`fishes`)이 "수집 가능한 어종 목록"이라면, `catch_record`는 **사용자가 실제로 잡아 인증한 기록**이다. 도감 화면은 `fishes` 전체를 나열하고, 각 어종마다 현재 사용자의 `catch_record` 존재 여부로 **획득(컬러) vs 미획득(그림자)** 을 그린다.

### 설계 — 인증 1건 = 1행 (옵션 B) ✅(확정)

- "감성돔을 3번 잡음"은 `catch_count` 컬럼이 아니라 **행 3개**로 표현한다.
  - **잡은 횟수** = `(user_id, fishes_id)`로 묶은 행의 개수(`COUNT`).
  - **획득 여부** = 그 행이 **하나라도 있는지**(그림자 여부).
  - **인증 사진 목록** = 그 행들의 `certified_image_url`.
- 집계값(`catch_count`·`completion_rate`)을 **저장하지 않고 파생**한다 → 사진 추가/삭제 시 숫자 동기화 버그가 원천 차단. 도메인 규모가 작아 `COUNT` 비용은 무시 가능. 나중에 "대표 사진"·"최초 획득" 같은 (user,fish)당 값이 필요해지면 헤더 테이블로 승격(= 옵션 A).
- `size`(cm)는 인증 시 **필수(NOT NULL)** 로 기록한다. 이번 조회 응답엔 노출하지 않고 **추후 크기 랭킹**의 기준으로 적재만 한다. 동점 처리를 위해 정수가 아닌 실수(`Double`).
- `user_id`는 인증(JWT)·`User` 엔티티 도입 전이라 **임시 plain Long**(FK 관계 아님). 도입 시 로그인 사용자에서 채우고 `@ManyToOne User`로 승격.

### `GET /api/collections` — 특정 어종의 내 인증 요약 ✅

특정 어종에 대해 내가 인증한 **사진 목록 + 잡은 횟수**를 반환한다. 도감에서 어종(그림자/컬러)을 눌렀을 때의 상세용.

- 파라미터: `userId`(임시, 추후 로그인 토큰으로 대체), `fishId`(전체 도감 어종 id).
- 안 잡은 어종이어도 **404가 아니라 200 + `catchCount:0`·`imageUrls:[]`** — 어종은 도감에 존재하고 "0번 잡음"이 맞기 때문(단건 리소스 조회인 `GET /api/fish/{id}`의 404와 다름).

요청: `GET /api/collections?userId=1&fishId=1`

```json
{
  "success": true,
  "code": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "catchCount": 3,
    "imageUrls": [
      "https://.../photo1.jpg",
      "https://.../photo2.jpg",
      "https://.../photo3.jpg"
    ]
  }
}
```

> ⚠️ **임시 사항:** `userId`를 파라미터로 받는 건 인증 미구현 때문의 과도기 조치다. JWT 도입 후에는 `GET /api/collections/me?fishId=`처럼 **로그인 사용자에서 신원을 얻고** `userId` 파라미터는 제거한다(파라미터로 남기면 남의 도감을 조회할 수 있음).
> ⚠️ **쓰기(POST) 미구현:** 인증 사진을 저장하는 API가 아직 없어, 로컬에서는 `catch_record`에 수동 INSERT(또는 시드)해야 결과가 보인다. `size` NOT NULL 유의.

## 데이터 모델 (ERD)

> **⚠️ 초안 v0.2 — 수정 가능성 있음.** 아래 이미지가 현재 draft이며, 컬럼·관계는 도메인 구현과 함께 확정됩니다.
> 모든 엔티티는 `BaseTimeEntity`를 상속해 `createdAt`/`modifiedAt`을 가집니다(ERD에는 편의상 미표기, `@SuperBuilder` 사용 → `docs/conventions.md`).

![ERD 초안 v0.2](erd-v0.2.png)

### 엔티티 요약 (이미지 기준 v0.2)

| 테이블 | 역할 | 주요 컬럼 |
|---|---|---|
| `users` | 사용자 | `id`, `username`(email), `password_hash`, `name`, `nickname` |
| `fishes` | 어종(도감 기준) | `id`, `name`, `description`·`habitat`(콘텐츠 시드로 적재), `image_url`(s3, TBD), `rarity`(ENUM LOW/USUALLY/HIGH, TBD), `is_collectible`(default true, 도감 노출 여부) |
| `major_fish` | 스팟-어종 매핑(주요 어종, 구 `fish_sopt`) | `id`, `fishes_id`·`spots_id`(FK, 조합 UNIQUE), `season`(TBD) |
| `catch_record` | 사용자 도감(어종 인증 **1건=1행**, 구 `user_dex`) | `id`, `user_id`(임시 plain Long), `fishes_id`(FK), `certified_image_url`(s3), `size`(cm, NOT NULL·랭킹 기준). 잡은 횟수·획득 여부는 (user,fish) 행 **집계로 파생** → `catch_count`·`completion_rate` 컬럼 없음. `spot_id`(어느 스팟에서 인증)는 추후 추가(TBD) |
| `spots` | 낚시 스팟 | `id`, `name`, `lat`, `lot`, `prohibit` |

### spots (낚시 스팟) 🚧
바다낚시지수 API(15142486)에서 **불변 정보만** 추출해 시드 저장 → `docs/external.md` §1, `docs/geo.md`. (컬럼명은 ERD v0.2 기준)

| 컬럼 | 타입 | 제약 | 설명 | 출처 |
|---|---|---|---|---|
| `id` | BIGINT | PK, auto | 스팟 식별자 | (내부 생성) |
| `name` | VARCHAR | NOT NULL | 위치명(장소이름) | API `seafsPstnNm` |
| `lat` | FLOAT | NOT NULL | 위도 | API `lat` |
| `lot` | FLOAT | NOT NULL | 경도 | API `lot` |
| `prohibit` | BOOLEAN | NOT NULL | 낚시 금지 여부 | 서비스 운영값(API 아님) |

- 현재 **49행**(고유 위치명, 추후 추가 가능). 이름이 유일하므로 시드 upsert 기준 키로 사용 가능(UNIQUE 제약 부여 여부는 v0.2에서 미확정).
- 예보성 필드(낚시지수·날씨·물때·대상 어종)는 저장하지 않고 상세 조회 시 실시간 호출 → 위 "스팟 데이터 설계" 참고.

```
User(users) 1 ──< catch_record >── 1 Fish(fishes)   # 사용자 도감(어종 인증 1건=1행)
Spot(spots) 1 ──< major_fish >── 1 Fish(fishes)     # 스팟-어종 매핑
# (spot_id 로 "어느 스팟에서 인증했는지"는 추후 catch_record 에 추가 — 현재 미포함)
```