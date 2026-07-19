# ranking.md — 랭킹 시스템 명세

> 사용자 랭킹(순위) 기능의 단일 명세. 두 가지 **독립 기준**으로 순위를 매긴다. 코드 추가/변경 시 이 문서를 함께 갱신한다(`docs/conventions.md`). 현재 상태: **✅ 구현됨**(`domain/ranking`). 닉네임 표시만 User/JWT 도입 후로 유보(→ `docs/auth-followup.md`).

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

### ⚠️ 유일한 공백 — 사용자 표시 정보(닉네임)

랭킹 **리스트/Top3**는 "누가 몇 위인지"를 사람이 알아볼 수 있게 **닉네임(또는 이름/프로필)** 을 표시해야 한다. 그런데:

- `catch_record.user_id`는 아직 **`User` 엔티티/인증(JWT) 미구현**이라 FK가 아닌 **plain Long**이다(CatchRecord.java:39-41).
- 따라서 랭킹 집계 결과에 붙일 **닉네임을 조회할 테이블(`users`)이 아직 없다.**

이것은 "랭킹용 컬럼이 부족"한 게 아니라 **`User` 도메인 미구현**이라는 상위 의존성이다. 두 가지 진행 방식이 있다(→ [열린 결정 사항](#열린-결정-사항)):

- **(A) 선(先) 랭킹 / 후(後) 닉네임:** 지금은 응답에 `userId`만 담아 순위 로직·엔드포인트를 완성하고, `User` 도입 시 닉네임 필드만 채운다. (도감 API가 이미 임시 `userId`로 가는 것과 동일한 과도기 전략)
- **(B) 선(先) User 도메인:** `users` 테이블·엔티티를 먼저 만들고 `catch_record.user_id`를 FK로 승격한 뒤 랭킹을 구현.

> 결론: **점수 계산은 지금 가능**, **닉네임 표시는 `User` 도메인에 의존**. 나머지 명세는 (A)를 기본 가정으로 작성한다.

---

## 엔드포인트 설계 📋

`/api/rankings` 하위. 두 기준이 화면·응답 구조가 같으므로 **경로로 기준을 구분**한다.

| 상태 | Method | Path | 설명 | 인증 |
|---|---|---|---|---|
| ✅ | GET | `/api/rankings/completion` | 도감 완성도 랭킹(내 순위 + Top3 + 전체) | 공개(임시) |
| ✅ | GET | `/api/rankings/size` | 최대 어종 크기 랭킹(내 순위 + Top3 + 전체) | 공개(임시) |

**설계 결정**
- **경로 분리 vs 쿼리 파라미터:** `/completion`·`/size`로 **경로 분리**한다(쿼리 `?type=` 대안보다 캐시·문서화·권한 확장에 유리). 두 핸들러는 같은 응답 DTO를 공유하되 `metric` 필드로 어떤 기준인지 표기.
- **임시 `userId` 파라미터:** 도감 API와 동일하게 JWT 도입 전까지 `userId`를 쿼리로 받아 "본인 순위(`me`)"를 계산한다. JWT 도입 후 파라미터 제거 → 로그인 사용자에서 신원 획득.
- **본인 기록이 없을 때:** 한 번도 인증 안 한 사용자는 랭킹 목록에 없다. 이때 `me.rank`는 `null`, 점수는 0(완성도 0% / maxSize `null`)로 응답한다(404 아님 — "아직 순위 없음"은 정상 상태).
- **페이징 📋 TBD:** 초기엔 전체 반환. 사용자가 늘면 `page`/`size` 도입 및 `me`는 별도 계산(자기 순위는 페이지 밖에 있을 수 있으므로 항상 함께 반환).

### 공통 파라미터

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `userId` | Long | 선택 | 본인 순위(`me`) 계산용 임시 파라미터. 없으면 `me`는 `null`. JWT 도입 시 제거. |

---

## Request / Response 스키마 📋

모든 응답은 공통 래퍼 `BaseResponse<T>`로 감싼다(`docs/architecture.md`).

### `GET /api/rankings/completion?userId=1` — 완성도 랭킹

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
      "nickname": null,
      "caughtCount": 12,
      "completionRate": 41.4
    },
    "top3": [
      { "rank": 1, "userId": 7, "nickname": null, "caughtCount": 27, "completionRate": 93.1 },
      { "rank": 2, "userId": 3, "nickname": null, "caughtCount": 25, "completionRate": 86.2 },
      { "rank": 3, "userId": 9, "nickname": null, "caughtCount": 20, "completionRate": 69.0 }
    ],
    "rankings": [
      { "rank": 1, "userId": 7, "nickname": null, "caughtCount": 27, "completionRate": 93.1 },
      { "rank": 2, "userId": 3, "nickname": null, "caughtCount": 25, "completionRate": 86.2 }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `metric` | String | `"COMPLETION"` 고정 |
| `totalFishCount` | int | 전체 도감 어종 수(완성도 분모, `is_collectible=true` 총계) |
| `me` | object\|null | 본인 순위 블록. `userId` 미전달 시 `null` |
| `me.rank` | int\|null | 전체에서 내 순위. 기록 없으면 `null` |
| `me.caughtCount` | int | 내가 인증한 **고유** 수집대상 어종 수 |
| `me.completionRate` | double | `caughtCount / totalFishCount × 100` (소수 1자리) |
| `top3` | array | 상위 3명(`rankings`의 앞 3개와 동일 데이터) |
| `rankings` | array | 전체 순위 리스트(내림차순) |

### `GET /api/rankings/size?userId=1` — 크기 랭킹

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
      "nickname": null,
      "maxSize": 42.5
    },
    "top3": [
      { "rank": 1, "userId": 4, "nickname": null, "maxSize": 88.0 },
      { "rank": 2, "userId": 7, "nickname": null, "maxSize": 71.3 },
      { "rank": 3, "userId": 2, "nickname": null, "maxSize": 65.0 }
    ],
    "rankings": [
      { "rank": 1, "userId": 4, "nickname": null, "maxSize": 88.0 }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `metric` | String | `"SIZE"` 고정 |
| `me.maxSize` | double\|null | 내가 인증한 기록 중 최대 `size`(cm). 기록 없으면 `null` |
| `rankings[].maxSize` | double | 해당 사용자의 최대 크기 |

> 완성도 응답엔 `totalFishCount`가 있고 크기 응답엔 없다(분모 개념이 없으므로). 그 외 `metric`/`me`/`top3`/`rankings` 뼈대는 공유한다.

---

## 집계 쿼리 설계 📋

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

## 열린 결정 사항 (구현 전 확정 필요)

| # | 결정 | 내용 | 상태 |
|---|---|---|---|
| 1 | **사용자 표시 정보** | **(A) 지금은 `userId`만 응답(`nickname=null`)**, `User`/JWT 도입 후 닉네임 필드만 채운다. 도감 API와 동일한 과도기 전략. | ✅ 확정 |
| 2 | **동점(tie) 처리** | **공동 순위(1,1,3)** — 같은 점수면 같은 rank를 주고 그 수만큼 다음 순위를 건너뛴다. 보조 tie-breaker 없음. | ✅ 확정 |
| 3 | **랭킹 표시 범위** | 전체 반환 / 상위 N명 + 내 순위만 / 페이징 | 📋 TBD(초기 전체) |
| 4 | **본인 기록 0건** | `me.rank=null` + 점수 0/`null` (본 문서 가정) | 📋 확정 대기 |
| 5 | **크기 랭킹 기록 0건 사용자** | 랭킹 목록에서 제외(인증 기록이 있어야 순위) | 📋 확정 대기 |

### 공동 순위(1,1,3) 계산 규칙 ✅

정렬된 결과를 서비스 계층에서 순번 매길 때, **직전 항목과 점수가 같으면 rank를 유지**하고 다르면 **현재 인덱스+1**로 점프한다.

```
예) 점수 내림차순 [93.1, 93.1, 86.2, 86.2, 69.0]
     rank →       [ 1,    1,    3,    3,    5  ]
```

- 완성도는 `completionRate`(또는 동치인 `caughtCount`) 기준, 크기는 `maxSize` 기준으로 동점 판정.
- `me.rank`도 같은 규칙으로 산출하므로, 나와 점수가 같은 사람들과 공동 순위가 된다.

---

## 패키지 배치 (구현 시) 📋

`docs/architecture.md`의 새 도메인 체크리스트에 따라 `domain/ranking`으로 신설한다.

```
domain/ranking
├─ controller/RankingController.java     # GET /api/rankings/completion, /size
├─ service/RankingService.java · RankingServiceImpl.java
├─ dto/RankingResponse.java · RankingEntryResponse.java
└─ exception/RankingErrorCode.java        # 접두사 예: R001
```

- 집계는 `collection`의 `CatchRecordRepository`(기존)와 `fish`의 `FishRepository`(기존, 전체 어종 수)를 **인터페이스/레포지토리 재사용**하거나, 랭킹 전용 집계 쿼리를 `CatchRecordRepository`에 추가한다.
- 엔티티는 새로 만들지 않는다(파생 집계만). → `entity`/신규 테이블 없음.
