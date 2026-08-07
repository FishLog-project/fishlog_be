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

- [ ] **`catch_record.user_id` FK 승격:** 임시 plain Long → `@ManyToOne User` 관계로 변경(`CatchRecord.java`). 기존 행의 `user_id` 값 보존·마이그레이션 포함. **(아직 미완 — 신원 전환과 별개인 스키마 변경으로 남겨둠)**
- [x] **보호 엔드포인트에 인증 적용:** Spring Security + JWT 필터 도입 후, 아래 "보호" 대상 엔드포인트에 인증 요구. → `SecurityConfig`
- [x] **`GlobalExceptionHandler` 확장:** `AuthenticationException`(401)·`AccessDeniedException`(403) 핸들러 추가.

## 2. 사용자 도감 (collection) — ✅ 인증(토큰) 전환 완료

- [x] **엔드포인트 신원 전환:** `userId` 쿼리 파라미터 **제거**, 컨트롤러가 `@AuthenticationPrincipal Long userId`로 로그인 사용자에서 신원 획득. (경로는 팀 결정으로 **현행 유지** — `GET /api/collections?fishId=`, `GET /api/collections/dex`. `/me` 리네임은 채택하지 않음.)
  - 효과: `userId` 파라미터 제거로 **남의 도감 조회(IDOR)** 차단.
- [x] **인증 정책 전환:** 공개(임시) → **보호**(SecurityConfig의 `anyRequest().authenticated()`로 인증 필수, 토큰 누락/무효 시 401).
- [ ] (연관·별개 트랙) `POST /api/collections/verify` 인증 사진 업로드(S3) 구현 → 보호. `size` NOT NULL 유의(→ `docs/media.md`).

## 3. 랭킹 (ranking) — ✅ 완료

- [x] **닉네임 채우기:** 응답의 `nickname`을 `users`에서 조회해 매핑(`RankingServiceImpl`, `UserRepository.findAllById`로 일괄 조회). 사용자를 찾지 못하면 `null`. → 랭킹 결정 #1(A) 완료.
- [x] **`userId` 파라미터 제거:** `me`(본인 순위) 계산을 `@AuthenticationPrincipal Long userId`(로그인 사용자) 기준으로 전환.
- [x] **인증 정책 확정:** **랭킹 목록·Top3는 공개**(`GET /api/rankings/**` permitAll), **`me` 블록만 로그인 시 채움**. 비로그인 요청이면 `me=null`.

---

## 완료 판정

§2·§3는 처리 완료돼 도감·랭킹에서 "임시 `userId`" 파라미터 흔적이 사라졌다. **남은 항목은 §1의 `catch_record.user_id` FK 승격 하나**(스키마 마이그레이션)로, 이것까지 끝나면 이 문서를 아카이브(또는 삭제)한다.
