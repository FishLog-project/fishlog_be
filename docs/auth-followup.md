# auth-followup.md — 인증(User/JWT) 완성 후 후속 작업

> **목적:** `User` 도메인(`users` 테이블·엔티티)과 JWT 인증이 **다른 팀원 작업으로 확정·병합되면**, 그동안 "임시 `userId`(plain Long)"로 우회해 둔 부분을 이어서 마무리하기 위한 **후속 작업 체크리스트**다.
>
> 각 도메인 문서(`docs/spec.md`, `docs/ranking.md`, `docs/security.md`)에 인라인 ⚠️로 흩어진 조치를 **한곳에 모은 것**이며, 이 문서가 후속 작업의 단일 진입점이다. 인증 방식·정책 원본은 `docs/security.md`.

## 전제 (다른 팀원 도메인에서 제공되어야 하는 것)

이 문서의 작업은 아래가 갖춰진 뒤 시작한다.

- [ ] `users` 테이블 + `User` 엔티티 (식별자 `id`, 표시용 `nickname` 등) — `docs/security.md`, `docs/product.md`
- [ ] JWT 발급/검증 + 인증 필터 체인(`global/security`) — `docs/security.md`, `docs/architecture.md`
- [ ] 로그인 사용자 신원을 컨트롤러에서 얻는 방법(예: `@AuthenticationPrincipal`) 확립

> 위가 확정되기 전에는 현재의 임시 `userId` 파라미터 방식을 그대로 둔다(도감·랭킹 모두 동작함).

---

## 1. 공통 (모든 사용자 소유 도메인)

- [ ] **`catch_record.user_id` FK 승격:** 임시 plain Long → `@ManyToOne User` 관계로 변경(`CatchRecord.java`). 기존 행의 `user_id` 값 보존·마이그레이션 포함.
- [ ] **보호 엔드포인트에 인증 적용:** Spring Security + JWT 필터 도입 후, 아래 "보호" 대상 엔드포인트에 인증 요구.
- [ ] **`GlobalExceptionHandler` 확장:** `AuthenticationException`(401)·`AccessDeniedException`(403) 핸들러 추가(현재 미구현 메모 → `docs/architecture.md`).

## 2. 사용자 도감 (collection) — 이미 구현됨, 인증 전환만 남음

- [ ] **엔드포인트 신원 전환:** `GET /api/collections?userId=&fishId=` → `GET /api/collections/me?fishId=`. `userId` 파라미터 **제거**하고 로그인 사용자에서 신원 획득.
  - 이유: `userId`를 파라미터로 받으면 **남의 도감을 조회**할 수 있음(→ `docs/spec.md` §사용자 도감 ⚠️).
- [ ] **인증 정책 전환:** 공개(임시) → **보호**.
- [ ] (연관·별개 트랙) `POST /api/collections/verify` 인증 사진 업로드(S3) 구현 → 보호. `size` NOT NULL 유의(→ `docs/media.md`).

## 3. 랭킹 (ranking)

- [ ] **닉네임 채우기:** 응답의 `nickname`(현재 `null`)을 `users`에서 조회해 매핑 → 랭킹 결정 #1(A) 완료 처리(→ `docs/ranking.md`).
- [ ] **`userId` 파라미터 제거:** `me`(본인 순위) 계산을 파라미터 대신 **로그인 사용자** 기준으로 전환.
- [ ] **인증 정책 확정:** 랭킹 목록 조회 자체는 공개 유지 가능하나, `me` 블록만 로그인 필요로 분리할지 결정.

---

## 완료 판정

위 3개 섹션 체크박스가 모두 처리되면 "임시 `userId`" 흔적이 코드에서 사라진 상태가 된다. 완료 시 각 원본 문서의 ⚠️/임시 표기를 걷어내고 이 문서를 아카이브(또는 삭제)한다.
