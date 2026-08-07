# ranking.md — 랭킹 시스템 명세

> 사용자 랭킹(순위) 기능의 단일 명세. 두 가지 **독립 기준**으로 순위를 매긴다. 코드 추가/변경 시 이 문서를 함께 갱신한다(`docs/conventions.md`). 현재 상태: **✅ 구현 완료**(`domain/ranking`) — 순위 집계·본인 순위(`me`)·닉네임 표시까지 모두 동작한다.

## 개요

두 개의 랭킹 기준을 제공한다. **화면 구성(와이어프레임)은 두 기준이 동일**하고, 순위를 매기는 **점수(metric)만 다르다.**

| 기준 | metric | 산출식 | 데이터 출처 |
|---|---|---|---|
| **도감 완성도** | `completionRate` | 내가 인증한 **고유 수집대상 어종 수 ÷ 전체 도감 어종 수** | `catch_record` 집계 + `fishes` 총계 |
| **최대 어종 크기** | `maxSize` (cm) | 내가 인증한 기록 중 **`size` 최댓값** | `catch_record.size` 집계 |

와이어프레임이 요구하는 화면 요소(두 기준 공통):

1. **본인 순위** — 내 rank(전체에서 몇 위)
2. **본인 점수** — 완성도 화면: 완성도 %(내가 잡은 어종 수 / 전체 어종 수) · 크기 화면: 내 최대 크기(cm)
3. **완성도/크기 Top 3** — 상위 3명
4. **전체 사용자 순위** — 전체 랭킹 리스트

---

## 구현 가능성 결론 ✅ (핵심)

**두 랭킹의 점수(metric) 계산은 현재 스키마로 100% 가능하다. 새 데이터 컬럼이 필요 없다.**

| 필요한 것 | 현재 상태 | 판정 |
|---|---|---|
| 완성도 분자(내 고유 어종 수) | `catch_record`에서 `COUNT(DISTINCT fishes_id)` (옵션 B 설계) | ✅ 가능 |
| 완성도 분모(전체 어종 수) | `fishes WHERE is_collectible=true`의 `COUNT` (기존 `FishRepository` 재사용) | ✅ 가능 |
| 크기 점수(내 최대 크기) | `catch_record.size`가 이미 **`NOT NULL Double`**로 적재 중(CatchRecord.java) | ✅ 가능 |
| 사용자 목록 집계 | `catch_record`를 `user_id`로 `GROUP BY` | ✅ 가능 |

> `size` 컬럼은 명세 v0.2에서 **"추후 크기 랭킹 기준"으로 이미 심어둔 것**이라(spec.md §catch_record), 랭킹을 위해 추가 마이그레이션이 필요 없다.

### 사용자 표시 정보(닉네임) ✅ 해결됨

랭킹 **리스트/Top3**는 "누가 몇 위인지"를 사람이 알아볼 수 있게 **닉네임**을 표시해야 한다. `User` 도메인·JWT가 병합되면서 이 요구는 충족됐다.

- `RankingServiceImpl`이 랭킹에 오른 `userId` 목록을 모아 `UserRepository.findAllById()`로 **한 번에** 닉네임을 조회해 채운다(집계당 쿼리 1회, N+1 없음). 사용자를 찾지 못하면 해당 항목의 `nickname`은 `null`.
- 기록이 0건인 로그인 사용자의 `me` 블록은 `findById()`로 닉네임만 따로 채운다.

> `catch_record.user_id`는 여전히 **plain Long**(FK 관계 아님)이지만, 닉네임을 `users`에서 별도 조회해 채우므로 랭킹 표시에는 지장이 없다. `@ManyToOne User` 승격은 스키마 마이그레이션이라 별도 작업으로 남아 있다 → `docs/auth-followup.md` §1.

---

## 엔드포인트 설계 ✅

`/api/rankings` 하위. 두 기준이 화면·응답 구조가 같으므로 **경로로 기준을 구분**한다.

| 상태 | Method | Path | 설명 | 인증 |
|---|---|---|---|---|
| ✅ | GET | `/api/rankings/completion` | 도감 완성도 랭킹(내 순위 + Top3 + 전체) | 목록 공개 / `me`는 로그인 시 |
| ✅ | GET | `/api/rankings/size` | 최대 어종 크기 랭킹(내 순위 + Top3 + 전체) | 목록 공개 / `me`는 로그인 시 |

**설계 결정**
- **경로 분리 vs 쿼리 파라미터:** `/completion`·`/size`로 **경로 분리**한다(쿼리 `?type=` 대안보다 캐시·문서화·권한 확장에 유리). 두 핸들러는 같은 응답 DTO를 공유하되 `metric` 필드로 어떤 기준인지 표기.
- **본인 순위(`me`) 신원:** ✅ **로그인 사용자(`@AuthenticationPrincipal Long userId`)** 기준으로 계산한다. 랭킹 목록·Top3는 공개(`permitAll`)라 비로그인도 조회 가능하지만, `Authorization: Bearer` 토큰을 함께 보내면 그 사용자의 `me`가 추가로 채워진다(토큰 없으면 `me=null`). 과거의 임시 `userId` 쿼리 파라미터는 제거됨.
- **본인 기록이 없을 때:** 한 번도 인증 안 한 사용자는 랭킹 목록에 없다. 이때 `me.rank`는 `null`, 점수는 0(완성도 0% / maxSize `null`)로 응답한다(404 아님 — "아직 순위 없음"은 정상 상태).
- **페이징 📋 TBD:** 초기엔 전체 반환. 사용자가 늘면 `page`/`size` 도입 및 `me`는 별도 계산(자기 순위는 페이지 밖에 있을 수 있으므로 항상 함께 반환).

### 공통 파라미터

쿼리 파라미터는 없다. 본인 순위(`me`)는 요청 헤더 `Authorization: Bearer {accessToken}`의 로그인 사용자로 계산하며, 토큰이 없으면 `me=null`이다.

---

## Request / Response 스키마 ✅

모든 응답은 공통 래퍼 `BaseResponse<T>`로 감싼다(`docs/architecture.md`).

### `GET /api/rankings/completion` — 완성도 랭킹

> 공개. `Authorization: Bearer {accessToken}`을 함께 보내면 `me`가 채워지고, 없으면 `me: null`.

```json
{
  "success": true,
  "code": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "metric": "COMPLETION",
    "totalFishCount": 29,
    "me": {
      "rank": 5,
      "userId": 1,
      "nickname": "붕어킬러",
      "caughtCount": 12,
      "completionRate": 41.4
    },
    "top3": [
      { "rank": 1, "userId": 7, "nickname": "낚시왕", "caughtCount": 27, "completionRate": 93.1 },
      { "rank": 2, "userId": 3, "nickname": "월척각", "caughtCount": 25, "completionRate": 86.2 },
      { "rank": 3, "userId": 9, "nickname": "바다사랑", "caughtCount": 20, "completionRate": 69.0 }
    ],
    "rankings": [
      { "rank": 1, "userId": 7, "nickname": "낚시왕", "caughtCount": 27, "completionRate": 93.1 },
      { "rank": 2, "userId": 3, "nickname": "월척각", "caughtCount": 25, "completionRate": 86.2 }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `metric` | String | `"COMPLETION"` 고정 |
| `totalFishCount` | int | 전체 도감 어종 수(완성도 분모, `is_collectible=true` 총계) |
| `me` | object\|null | 본인 순위 블록. **비로그인(토큰 미전달) 시 `null`** |
| `me.rank` | int\|null | 전체에서 내 순위. 기록 없으면 `null` |
| `me.caughtCount` | int | 내가 인증한 **고유** 수집대상 어종 수 |
| `me.completionRate` | double | `caughtCount / totalFishCount × 100` (소수 1자리) |
| `*.nickname` | String\|null | `users`에서 조회한 닉네임. 사용자를 찾지 못하면 `null` |
| `top3` | array | 상위 3명(`rankings`의 앞 3개와 동일 데이터) |
| `rankings` | array | 전체 순위 리스트(내림차순) |

### `GET /api/rankings/size` — 크기 랭킹

> 공개. `Authorization: Bearer {accessToken}`을 함께 보내면 `me`가 채워지고, 없으면 `me: null`.

완성도와 **구조 동일**, 점수 필드만 다르다.

```json
{
  "success": true,
  "code": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "metric": "SIZE",
    "me": {
      "rank": 8,
      "userId": 1,
      "nickname": "붕어킬러",
      "maxSize": 42.5
    },
    "top3": [
      { "rank": 1, "userId": 4, "nickname": "감성돔장인", "maxSize": 88.0 },
      { "rank": 2, "userId": 7, "nickname": "낚시왕", "maxSize": 71.3 },
      { "rank": 3, "userId": 2, "nickname": "새벽출조", "maxSize": 65.0 }
    ],
    "rankings": [
      { "rank": 1, "userId": 4, "nickname": "감성돔장인", "maxSize": 88.0 }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `metric` | String | `"SIZE"` 고정 |
| `me.maxSize` | double\|null | 내가 인증한 기록 중 최대 `size`(cm). 기록 없으면 `null` |
| `rankings[].maxSize` | double | 해당 사용자의 최대 크기 |
| `*.nickname` | String\|null | `users`에서 조회한 닉네임. 사용자를 찾지 못하면 `null` |

> 완성도 응답엔 `totalFishCount`가 있고 크기 응답엔 없다(분모 개념이 없으므로). 그 외 `metric`/`me`/`top3`/`rankings` 뼈대는 공유한다.

---

## 집계 쿼리 설계 ✅

레포지토리 계층에서 JPQL/`@Query`로 사용자별 점수를 한 번에 집계한다. (도메인 규모가 작아 실시간 집계로 충분 — 랭킹 스냅샷 테이블은 사용자 급증 시 재검토)

**완성도 랭킹 (사용자별 고유 어종 수)**

```sql
SELECT cr.user_id AS userId, COUNT(DISTINCT cr.fishes_id) AS caughtCount
FROM catch_record cr
JOIN fishes f ON f.id = cr.fishes_id AND f.is_collectible = true
GROUP BY cr.user_id
ORDER BY caughtCount DESC;
-- 분모(totalFishCount)는 FishRepository로 별도 조회: COUNT(*) WHERE is_collectible = true
```

**크기 랭킹 (사용자별 최대 크기)**

```sql
SELECT cr.user_id AS userId, MAX(cr.size) AS maxSize
FROM catch_record cr
GROUP BY cr.user_id
ORDER BY maxSize DESC;
```

- **완성도 분자는 반드시 `DISTINCT fishes_id`**: 옵션 B에서 같은 어종 3번 인증 = 3행이지만 도감 완성도는 1칸이다. `DISTINCT` 없으면 완성도가 과대 계산된다.
- **완성도 분자도 `is_collectible=true`만**: `기타어종` 같은 비수집 종은 전체 도감 분모에 없으므로 분자에서도 제외해야 완성도 100% 초과가 안 생긴다.
- **rank 부여**: DB `RANK()` 윈도우 함수 대신, 정렬된 결과를 **서비스 계층에서 순번 매김**(동점 처리 정책을 코드로 명시하기 위함, 아래).

---

## 결정 사항

| # | 결정 | 내용 | 상태 |
|---|---|---|---|
| 1 | **사용자 표시 정보** | `users`에서 닉네임을 조회해 채운다(`UserRepository.findAllById`로 일괄 조회). 사용자를 찾지 못하면 `null`. | ✅ 구현됨 |
| 2 | **동점(tie) 처리** | **공동 순위(1,1,3)** — 같은 점수면 같은 rank를 주고 그 수만큼 다음 순위를 건너뛴다. 보조 tie-breaker 없음. | ✅ 구현됨 |
| 3 | **랭킹 표시 범위** | 초기엔 **전체 반환**. 사용자 증가 시 페이징 도입 재검토(아래 "엔드포인트 설계" 참고). | 📋 TBD(현재 전체) |
| 4 | **본인 기록 0건** | 랭킹 목록엔 없으므로 `me.rank=null` + 완성도 `0.0` / `maxSize=null`. 404가 아니라 200. | ✅ 구현됨 |
| 5 | **크기 랭킹 기록 0건 사용자** | 랭킹 목록에서 제외(인증 기록이 있어야 순위에 오른다). | ✅ 구현됨 |
| 6 | **비로그인 요청** | 목록·Top3는 그대로 반환하고 `me=null`. (`GET /api/rankings/**`는 `permitAll`) | ✅ 구현됨 |

### 공동 순위(1,1,3) 계산 규칙 ✅

정렬된 결과를 서비스 계층에서 순번 매길 때, **직전 항목과 점수가 같으면 rank를 유지**하고 다르면 **현재 인덱스+1**로 점프한다.

```
예) 점수 내림차순 [93.1, 93.1, 86.2, 86.2, 69.0]
     rank →       [ 1,    1,    3,    3,    5  ]
```

- 완성도는 `completionRate`(또는 동치인 `caughtCount`) 기준, 크기는 `maxSize` 기준으로 동점 판정.
- `me.rank`도 같은 규칙으로 산출하므로, 나와 점수가 같은 사람들과 공동 순위가 된다.

---

## 패키지 배치 ✅

`docs/architecture.md`의 새 도메인 체크리스트에 따라 `domain/ranking`으로 신설했다.

```
domain/ranking
├─ controller/RankingController.java · RankingControllerSpec.java   # GET /api/rankings/completion, /size
├─ service/RankingService.java · RankingServiceImpl.java
└─ dto/RankingResponse.java · RankingEntryResponse.java · RankingType.java
```

- **`exception` 패키지 없음:** 랭킹은 조회 전용이고 "순위 없음"도 정상 상태(200)라 **도메인 에러 코드가 필요한 실패 경로가 없다.** `docs/architecture.md`의 "없는 레이어의 빈 패키지는 만들지 않는다" 규칙에 따라 만들지 않았다. 실패 케이스가 생기면 그때 `RankingErrorCode`(접두사 `R001`)를 추가한다.
- **집계 쿼리는 `CatchRecordRepository`에 추가**했다(`findCompletionScores`·`findMaxSizeScores`, projection은 `UserFishCount`·`UserMaxSize`). 완성도 분모는 `FishRepository.countByIsCollectibleTrue()`, 닉네임은 `UserRepository.findAllById()`로 조회한다.
- 엔티티는 새로 만들지 않는다(파생 집계만). → `entity`/신규 테이블 없음.

> ⚠️ **레이어 규칙 예외:** `RankingServiceImpl`이 `collection`·`fish`·`user`의 **리포지토리를 직접** 참조한다. `docs/architecture.md`의 "상대 도메인의 repository·entity에 직접 접근하지 않는다" 규칙과 어긋나는 지점으로, 랭킹이 순수 파생 집계라 서비스 경유가 과했다는 판단에서 나온 의도적 선택이다. 도메인 간 결합이 더 늘어나면 각 도메인의 조회 서비스 인터페이스 경유로 정리한다 📋.
