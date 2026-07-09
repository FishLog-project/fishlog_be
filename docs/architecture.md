# architecture.md — 패키지 구조·레이어·공통 패턴

> 이 문서는 CLAUDE.md에서 항상 자동 로드됩니다. 현재 코드는 최소(health 엔드포인트) 상태라, 아래 상당 부분은 **📋 계획(TBD)** 이며 도메인 구현과 함께 확정합니다.

## 현재 구조 ✅

```
com.fishlog.fishlog_be
├─ FishlogBeApplication.java     # @SpringBootApplication 진입점
├─ controller
│  └─ HealthController.java      # GET /api/health → {"status":"ok"}
└─ global
   ├─ response
   │  └─ BaseResponse.java       # 공통 응답 래퍼 <T>
   └─ exception
      ├─ model
      │  └─ BaseErrorCode.java   # 에러 코드 인터페이스 (code/message/status)
      ├─ GlobalErrorCode.java    # 전역 에러 코드 enum (G001~G006)
      ├─ CustomException.java    # BaseErrorCode 기반 비즈니스 예외
      ├─ TooManyRequestsException.java  # 429 + retryAfterSec 전용 예외
      └─ GlobalExceptionHandler.java    # @RestControllerAdvice 전역 핸들러
```

- 모든 API는 `/api` 베이스 경로 아래에 둡니다.
- API 문서: SpringDoc OpenAPI → Swagger UI `/swagger-ui.html`.

## 목표 레이어링 📋

도메인별 패키지 + 레이어 분리를 지향합니다 (확정 전 초안):

```
com.fishlog.fishlog_be
├─ global            # 공통: 응답 포맷, 예외, 설정, 보안, 유틸
│  ├─ response       #   BaseResponse 등 공통 응답 래퍼
│  ├─ config         #   Spring 설정 (Web, JPA, S3, Swagger 등)
│  ├─ exception      #   전역 예외 처리 (@RestControllerAdvice)
│  └─ security       #   JWT, 인증 필터 (docs/security.md)
└─ domain
   ├─ user           # 회원/인증
   ├─ spot           # 낚시 스팟 (좌표·주변 검색 → docs/geo.md)
   ├─ fish           # 어종 정보
   ├─ collection     # 어종 도감·사진 인증 (게이미피케이션)
   ├─ tour           # 주변 관광 시설
   └─ ...            # 각 domain: controller / service / repository / entity / dto
```

## 공통 응답 포맷 ✅

모든 API 응답은 `global/response/BaseResponse<T>`로 감쌉니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 요청 성공 여부 |
| `code` | int | HTTP 상태 코드 (예: 200, 404) |
| `message` | String | 응답 메시지 |
| `data` | T | 응답 데이터 (에러 시 `null`) |

정적 팩토리로 생성합니다:

```java
BaseResponse.success(data);              // 200, 기본 성공 메시지
BaseResponse.success("생성 완료", data);   // 200, 커스텀 메시지
BaseResponse.error(404, "메시지");         // 실패 응답 (보통 핸들러가 사용)
```

- 컨트롤러는 성공 시 `BaseResponse.success(...)`를 반환합니다.
- 실패 응답은 직접 만들지 말고 **예외를 던져** `GlobalExceptionHandler`가 변환하도록 합니다(아래).
- `@Schema` 문서화가 붙어 있어 Swagger UI에 응답 형식이 노출됩니다.

## 예외 처리 ✅

전역 `@RestControllerAdvice`(`global/exception/GlobalExceptionHandler`) + 에러 코드 enum 방식입니다.

**에러 코드 체계**
- `model/BaseErrorCode` 인터페이스: `getCode()` / `getMessage()` / `getStatus()`.
- `GlobalErrorCode`(enum, `G001~G006`)가 이를 구현 — 공통 에러(입력값/리소스/서버/인증/권한/메서드).
- **도메인 에러**는 `domain/{xxx}/exception/XxxErrorCode`(enum)가 `BaseErrorCode`를 구현해 추가합니다(코드 접두사는 도메인별로: 예 `U001`, `S001`).

**예외 발생 → 응답 변환**
```java
// 비즈니스 예외: 에러 코드만 던지면 상태/메시지가 자동 매핑됨
throw new CustomException(GlobalErrorCode.RESOURCE_NOT_FOUND);
// 429 (요청 빈도 제한): 남은 대기 시간을 data.retryAfterSec로 전달
throw new TooManyRequestsException("잠시 후 다시 시도해주세요.", 30);
```

`GlobalExceptionHandler`가 처리하는 주요 예외:

| 예외 | HTTP | 비고 |
|---|---|---|
| `CustomException` | 에러 코드의 status | 비즈니스 예외 (도메인 포함) |
| `MethodArgumentNotValidException` | 400 | `@Valid` 필드 검증 실패, 필드별 메시지 병합 |
| `HttpMessageNotReadableException` | 400 | 요청 본문 파싱 실패 |
| `MethodArgumentTypeMismatch` / `MissingServletRequestParameter` | 400 | 파라미터 오류 |
| `TooManyRequestsException` | 429 | `data.retryAfterSec` 포함 |
| `HttpRequestMethodNotSupportedException` | 405 | 허용되지 않은 메서드 |
| `NoHandlerFoundException` | 404 | 매핑 없는 경로 |
| `SQLTransientConnectionException` | 503 | DB 커넥션 풀 타임아웃 |
| `Exception` (fallback) | 500 | 예상치 못한 서버 오류 |

> **미구현(도입 시 추가):** Spring Security(`AuthenticationException` 401 / `AccessDeniedException` 403), Redis(`RedisConnectionFailureException` 등 503) 핸들러는 해당 의존성 추가 후 넣습니다. 핸들러 내부에 안내 주석이 있습니다. (참고: Sportize `be/global/exception/GlobalExceptionHandler.java`)

## 설정(Profile) 구성 ✅/📋

- Profile: `local`, `prod` (properties는 `be_config` 서브모듈, `docs/setup.md` 참고).
- 테스트는 DB 없이 실행 (`src/test/resources/application.yml`에서 DataSource/Hibernate 자동설정 제외).

> 위 구조가 확정되면 배지를 ✅로 갱신하고, 실제 패키지 트리와 동기화하세요.