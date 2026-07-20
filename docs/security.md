# security.md — 인증·인가

> 방향 **확정:** 자체 이메일/비밀번호 회원가입·로그인 + JWT(Access/Refresh 회전). 이메일 인증코드(6자리) 검증을 거쳐 가입한다. 아래는 그 **구현 명세**이며, 세부 파라미터(만료·정책 수치)는 조정 가능한 **제안값**으로 표기한다.
>
> 관련 문서: 엔드포인트·Request/Response·`users` 모델 → `docs/spec.md` / 패키지 배치 → `docs/architecture.md` / 환경변수·서브모듈 → `docs/setup.md`.
>
> **구현 현황:** 이메일 인증코드 발송·확인(§1-1·§1-2), 회원가입 완료(§1-3), 로그인·토큰 재발급·로그아웃 엔드포인트(§2), Security/JWT 인프라(`global/security`·`global/jwt`), BCrypt 인코더, 예외 핸들러(401/403/503) **모두 구현 완료 ✅**.

## 1. 회원가입 흐름 (이메일 인증 → 가입)

사용자는 **① 이메일 인증코드 발송 → ② 코드 확인 → ③ 비밀번호·닉네임 입력으로 가입** 3단계를 순서대로 거친다. 각 단계는 별도 API이며, 앞 단계의 통과 사실은 **Redis에 저장된 인증완료 플래그**로 다음 단계에 전달된다(가입 폼 상태를 서버가 신뢰하지 않기 위함).

```
[1] POST /api/auth/email/send-code   { email }
      └ 미가입 이메일 검증 → 6자리 코드 생성 → Redis 저장(TTL 5분) → 메일 발송
[2] POST /api/auth/email/verify-code  { email, code }
      └ 코드 대조 → 일치 시 "인증완료" 플래그 설정(TTL 10분), 코드 소비
[3] POST /api/auth/signup             { email, password, nickname }
      └ 인증완료 플래그 확인·소비 → 비밀번호 BCrypt 해시 → User 저장 (토큰 미발급 → 이후 로그인)
```

### 1-1. 인증코드 발송 (`send-code`) ✅ 구현됨
- **도메인 제한(선택):** `auth.allowed-email-domains`가 설정돼 있으면 그 도메인 이메일만 허용, 아니면 `EMAIL_DOMAIN_NOT_ALLOWED`. **값이 비어 있으면 제한 없음**(기본).
- **사전 검증:** 이미 가입된 이메일이면 거부(`EMAIL_ALREADY_EXISTS`). 중복 판정은 `UserRepository.existsByUsername`.
- **코드:** 숫자 **6자리**(`000000`~`999999`), `SecureRandom`으로 생성. 앞자리 0 유지(문자열 저장).
- **저장:** Redis `auth:email:code:{email}` = 코드, **TTL 5분**(입력 제한시간). 발송 시 이전 시도 카운터(`auth:email:attempts:{email}`) 초기화.
- **남용 방지(요청 빈도 제한):**
  - **재전송 쿨다운 30초** — `auth:email:resend:{email}` 존재 시 `429`(`retryAfterSec` 포함).
  - **시간당 발송 5회** — `auth:email:sendcount:{email}` INCR, 첫 발송 시 1시간 TTL. 초과 시 `429`.
- **메일 발송은 비동기**(`@Async` — `AsyncConfig`)로 처리해 응답 지연·실패가 흐름을 막지 않게 한다.
- **응답:** `codeTtlSeconds`(코드 유효시간, 초)를 돌려줘 클라이언트가 타이머를 표시.

### 1-2. 인증코드 확인 (`verify-code`) ✅ 구현됨
- Redis의 저장 코드와 대조.
  - 코드 없음(만료/미발송) → `VERIFICATION_CODE_EXPIRED`.
  - 불일치 → 시도 횟수(`auth:email:attempts:{email}`) 누적(첫 실패 시 코드 TTL만큼 만료 부여), `VERIFICATION_CODE_MISMATCH`.
  - **연속 5회 오입력 → 코드·시도 카운터 삭제(무효화)**(brute-force 방지). 재발송 필요.
- **일치 시:** 인증완료 플래그 `auth:email:verified:{email}` = true, **TTL 10분**(이 안에 가입 완료해야 함). 코드·시도 카운터는 삭제(소비).
- **응답:** `verifiedTtlSeconds`(인증완료 유지시간, 초).

### 1-3. 회원가입 완료 (`signup`) ✅ 구현됨
- **입력:** `email`, `password`, `nickname` (이름 `name`은 받지 않음 — `docs/spec.md` `users` 참조).
- **검증 순서:**
  1. `auth:email:verified:{email}` 플래그 존재 확인 → 없으면 `EMAIL_NOT_VERIFIED`.
  2. 이메일 재중복 확인(플래그 발급~가입 사이 선점 대비) → `EMAIL_ALREADY_EXISTS`.
  3. 닉네임 중복 확인 → `NICKNAME_ALREADY_EXISTS`.
  4. 비밀번호 정책 검증(§4).
- **처리:** 비밀번호를 **BCrypt** 해시로 저장, `User` 생성 → 인증완료 플래그 **소비(삭제)** → `{ userId, nickname }` 반환.
  - > **확정 ✅:** 가입 시에는 **토큰을 발급하지 않는다.** 가입 후 **로그인 API(§2-1)** 로 별도 로그인해 토큰을 발급받는다.

## 2. 로그인 · 토큰 (JWT) ✅ 구현됨

> JWT 인프라(`global/jwt`의 `JwtProvider`·`JwtAuthenticationFilter`, `global/security`)와 발급/재발급/로그아웃 **엔드포인트·refresh 저장 로직(`AuthService`, `auth:refresh:{userId}`)** 모두 구현됨.

### 2-1. 발급 구조 — Access + Refresh (회전)
로그인 성공 시 **Access + Refresh** 두 토큰을 발급한다.

| 토큰 | 용도 | 저장 위치 | 만료(제안) |
|---|---|---|---|
| **Access** | API 인증(`Authorization: Bearer`) | 클라이언트만 | 30분 |
| **Refresh** | Access 재발급 | 클라이언트 + **서버(Redis)** | 14일 |

- **서명:** HS256, 시크릿은 환경변수(`JWT_SECRET`, `be_config`). 클레임에 `sub`(userId)·`exp`·발급구분(access/refresh) 포함.
- **Refresh 서버 저장:** `auth:refresh:{userId}` = 발급된 refresh(또는 그 해시), TTL = refresh 만료. **사용자당 1개**(다중 기기 정책은 TBD).

### 2-2. 재발급 (`refresh`) — 회전(rotation)
- 클라이언트가 refresh 제출 → 서명·만료 검증 → **Redis 저장값과 일치 확인**(불일치 시 재사용/탈취로 간주해 거부·무효화).
- 검증 통과 시 **새 Access + 새 Refresh를 함께 발급**하고 Redis 값을 교체(구 refresh 무효화).

### 2-3. 로그아웃 (`logout`)
- Bearer 인증 상태에서 호출 → `auth:refresh:{userId}` 삭제(refresh 무효화). Access는 만료까지 유효(짧은 TTL로 위험 최소화; 블랙리스트 도입 여부는 TBD).

## 3. 인가 (엔드포인트 정책)

- **공개(인증 불필요):** 인증 API 전체(`/api/auth/**`), 낚시 스팟·어종 등 열람성 조회(`GET /api/spots`, `GET /api/fish` 등), Swagger(`/swagger-ui/**`·`/v3/api-docs/**`).
- **보호(인증 필요):** 어종 도감 인증(`POST /api/collections/verify`), 내 도감(`GET /api/collections/me`), 내 프로필 등 **사용자 소유 리소스**.
- 보호 리소스는 `Authorization: Bearer {accessToken}` 필수. 누락/무효 → `401`.
- **권한(Role) 구분은 현재 없음** — 전원 일반 사용자다. 관리자(`ADMIN`) 전용 기능(어종/스팟 마스터 데이터 관리 등)이 필요해지면 그때 `users.role` 컬럼과 함께 도입하고, JWT 클레임에 `role`을 추가한다(`403` 권한 부족 처리 포함).

## 4. 비밀번호 · 닉네임 정책 (제안)

- **비밀번호:** 최소 8자, **영문 + 숫자 포함**(특수문자 권장). 저장은 항상 BCrypt 해시(평문·양방향 암호화 금지).
- **닉네임:** 2~10자, 공백/특수문자 제한(허용 문자셋 TBD). **유니크 제약**(중복 불가) — DB `UNIQUE` + 가입 시 사전 검사.
- **이메일:** RFC 형식 검증, 유니크. 로그인 식별자(`users.username`).
- > 위 수치·문자셋은 제안값이며 팀 합의로 확정한다.

## 5. 패키지 배치 (→ `docs/architecture.md`)

| 위치 | 담는 것 | 상태 |
|---|---|---|
| `domain/user` | `User` 엔티티·`UserRepository`(`existsByUsername` 등) | ✅ |
| `domain/auth` | `AuthController`(send-code·verify-code·signup·login·refresh·logout)+`AuthControllerSpec`(Swagger), `EmailVerificationService`(코드 발송·확인, Redis)·`AuthService`(가입·로그인·재발급·로그아웃, refresh 저장), 인증 DTO(record)·`AuthErrorCode`, `mail/EmailSender` | ✅ |
| `global/jwt` | `JwtProvider`(발급·검증), `JwtAuthenticationFilter` | ✅ |
| `global/security` | `SecurityConfig`(필터 체인·공개/보호 경로), `CustomUserDetails(Service)`, `JwtAuthenticationEntryPoint`(`401`)·`JwtAccessDeniedHandler`(`403`) | ✅ |
| `global/config` | `RedisConfig`(인증코드 저장·예보 캐시), `PasswordConfig`(BCrypt 인코더), `AsyncConfig`(메일 비동기) | ✅ |

- 도메인 에러코드는 `domain/auth/exception/AuthErrorCode`(enum, `BaseErrorCode` 구현)로 `A0xx` 접두사 부여. **A001~A008 모두 구현됨 ✅:**
  - `A001 EMAIL_ALREADY_EXISTS`(409), `A002 EMAIL_NOT_VERIFIED`(400), `A003 VERIFICATION_CODE_EXPIRED`(400), `A004 VERIFICATION_CODE_MISMATCH`(400), `A005 NICKNAME_ALREADY_EXISTS`(409), `A006 INVALID_CREDENTIALS`(401), `A007 INVALID_REFRESH_TOKEN`(401), `A008 EMAIL_DOMAIN_NOT_ALLOWED`(400).

## 6. 예외 처리 (Spring Security) ✅ 구현됨

`GlobalExceptionHandler`에 아래 핸들러가 **구현되어 있다**(`docs/architecture.md` 예외 처리 표 참고).

| 예외 | HTTP | 비고 |
|---|---|---|
| `AuthenticationException` | 401 | 미인증·토큰 무효 |
| `AccessDeniedException` | 403 | 권한 부족 |
| `RedisConnectionFailureException` | 503 | 인증코드 저장소(Redis) 장애 |

- 인증코드 남용·재전송 초과는 `TooManyRequestsException`(429, `retryAfterSec`)을 재사용한다.

## 7. 필요한 설정·환경변수 (→ `docs/setup.md`, `be_config`)

| 키 | 용도 | 예시/기본 |
|---|---|---|
| `JWT_SECRET` | JWT 서명 시크릿 | (필수, 서브모듈) |
| `jwt.access-ttl-seconds` | Access 만료 | `1800` |
| `jwt.refresh-ttl-seconds` | Refresh 만료 | `1209600` |
| `spring.mail.*` | SMTP(인증코드 메일 발송) 호스트·계정 | (필수, 서브모듈) |
| `spring.data.redis.*` | Redis 접속(인증코드·refresh·예보 캐시 공용) | (필수) |
| `auth.email.code-ttl-seconds` | 코드 유효시간 | `300` |
| `auth.email.resend-cooldown-seconds` | 재전송 쿨다운 | `30` |
| `auth.email.hourly-send-limit` | 시간당 발송 한도 | `5` |
| `auth.email.max-verify-attempts` | 코드 오입력 허용 횟수 | `5` |
| `auth.email.verified-ttl-seconds` | 인증완료→가입 제한시간 | `600` |
| `auth.allowed-email-domains` | 가입 허용 이메일 도메인(쉼표 구분). **비우면 제한 없음** | (비어 있음) |

> **Preflight:** 위 필수 값(`JWT_SECRET`, SMTP, Redis)이 `be_config`에 세팅되지 않았으면 구현 작업을 **중단하고 사용자에게 세팅 요청**(`CLAUDE.md` Preflight).

## 8. 확정 필요 / 열린 항목

- [x] 가입 즉시 자동 로그인(토큰 발급) vs 별도 로그인 → **별도 로그인으로 확정**(가입 시 토큰 미발급)
- [ ] 다중 기기 로그인(사용자당 refresh 다건) 허용 여부 — 현재 1개
- [ ] 비밀번호 재설정(찾기) 흐름 도입 여부 — 도입 시 `auth:password:*` 네임스페이스로 동일 패턴 재사용
- [ ] Access 강제 무효화(로그아웃 즉시 차단) 필요 시 블랙리스트 도입
- [ ] 닉네임 허용 문자셋·비밀번호 세부 정책 수치 확정
