# architecture.md — 패키지 구조·레이어·공통 패턴

> 이 문서는 CLAUDE.md에서 항상 자동 로드됩니다. 인증(이메일 인증코드)·스팟·시드 적재와 Security/JWT 인프라가 구현되어 있고, 회원가입·로그인 등 일부 흐름은 아직 **📋 계획(TBD)** 입니다(각 항목 배지 참고).

## 현재 구조 ✅

```
com.fishlog.fishlog_be
├─ FishlogBeApplication.java     # @SpringBootApplication + @EnableJpaAuditing 진입점
├─ domain
│  ├─ auth                       # 인증 (이메일 인증코드 발송·확인 — 회원가입/로그인 흐름은 진행 중)
│  │  ├─ controller/AuthController.java        # POST /api/auth/email/{send-code,verify-code}
│  │  ├─ dto/                     # EmailSendCode{Request,Response}, EmailVerifyCode{Request,Response} (record)
│  │  ├─ exception/AuthErrorCode.java          # A001·A003·A004·A008
│  │  ├─ mail/EmailSender.java                 # 인증코드 메일 발송
│  │  └─ service/EmailVerificationService.java # Redis 기반 코드 발송·확인
│  ├─ user
│  │  ├─ entity/User.java  repository/UserRepository.java
│  ├─ spot
│  │  ├─ controller/SpotController.java  service/SpotService.java  dto/SpotResponse.java
│  │  ├─ entity/Spot.java  entity/MajorFish.java
│  │  └─ repository/SpotRepository.java  repository/MajorFishRepository.java
│  └─ fish
│     ├─ entity/Fish.java  entity/Rarity.java
│     └─ repository/FishRepository.java
└─ global
   ├─ common/BaseTimeEntity.java              # createdAt/modifiedAt 감사(auditing) 공통 상위 엔티티
   ├─ response/BaseResponse.java              # 공통 응답 래퍼 <T>
   ├─ config                                  # AsyncConfig(@Async), PasswordConfig(BCrypt), RedisConfig(캐시·인증 저장)
   ├─ jwt                                     # JwtProvider, JwtAuthenticationFilter
   ├─ security                                # SecurityConfig, CustomUserDetails(Service), JwtAuthenticationEntryPoint(401), JwtAccessDeniedHandler(403)
   ├─ init                                    # SeedDataInitializer, SpotSeedLoader, SeedDataReader (+dto) — 스팟/어종 시드 적재
   └─ exception
      ├─ model/BaseErrorCode.java             # 에러 코드 인터페이스 (code/message/status)
      ├─ GlobalErrorCode.java                 # 전역 에러 코드 enum (G001~G006)
      ├─ CustomException.java                 # BaseErrorCode 기반 비즈니스 예외
      ├─ TooManyRequestsException.java        # 429 + retryAfterSec 전용 예외
      └─ GlobalExceptionHandler.java          # @RestControllerAdvice 전역 핸들러
```

- 모든 API는 `/api` 베이스 경로 아래에 둡니다.
- API 문서: SpringDoc OpenAPI → Swagger UI `/swagger-ui.html`.

## 패키지 구조 처리 규칙 📋

> 아래는 **새 도메인/기능을 추가할 때 지켜야 하는 패키지 배치 규칙**입니다. `domain`에는 이미 `auth`·`user`·`spot`·`fish`가 있으며, 새 도메인 추가 시 이 규칙에 맞춰 패키지를 생성합니다.

### 최상위 2분할: `global` vs `domain`

```
com.fishlog.fishlog_be
├─ FishlogBeApplication.java   # 진입점 (@SpringBootApplication + @EnableJpaAuditing)
├─ global                      # 특정 도메인에 속하지 않는 공통·횡단(cross-cutting) 코드
└─ domain                      # 비즈니스 도메인별 코드 (도메인당 하위 패키지 1개)
```

- **판단 기준:** 특정 비즈니스 개념(스팟·어종·도감·유저 등)에 종속되면 `domain/{name}`, 여러 도메인이 공유하거나 인프라·기술 관심사이면 `global`에 둡니다.
- `controller`가 도메인 하위에 위치하므로, `architecture.md` 상단 "현재 구조"의 최상위 `controller` 패키지(HealthController)는 도메인이 없는 헬스체크 전용 예외로만 유지합니다. 새 API는 반드시 해당 `domain/{name}/controller`에 둡니다.

### 도메인 패키지 규칙 (`domain/{name}/…`)

도메인 하나는 아래 하위 패키지로 **레이어를 분리**합니다. 필요한 것만 만들고, 없는 레이어의 빈 패키지는 만들지 않습니다.

| 하위 패키지 | 필수 | 담는 것 | 네이밍 |
|---|---|---|---|
| `controller` | ✅ | REST 컨트롤러 + **Swagger 문서 인터페이스**. `@RestController`, `/api` 하위 매핑, `BaseResponse` 반환. Swagger 애너테이션은 `XxxControllerSpec` 인터페이스로 분리(아래 규칙) | `XxxController` / `XxxControllerSpec` |
| `service` | ✅ | 비즈니스 로직. **단일 구체 클래스**(`@Service`). 인터페이스+`Impl` 분리는 하지 않는다 | `XxxService` |
| `repository` | 상황 | Spring Data JPA 리포지토리 | `XxxRepository` |
| `entity` | 상황 | JPA 엔티티 + 그 도메인 전용 enum | `Xxx`, `XxxType`, `XxxStatus` |
| `dto` | ✅ | 요청/응답 DTO | `XxxRequest` / `XxxResponse` |
| `exception` | ✅ | 도메인 에러 코드 enum (`BaseErrorCode` 구현) | `XxxErrorCode` |
| `scheduler` | 선택 | 주기 실행(`@Scheduled`) 작업 | `XxxScheduler` |
| `event` | 선택 | 도메인 이벤트 + 리스너 | `XxxEvent` / `XxxEventListener` |
| `policy` | 선택 | 정책·규칙 계산 로직(가격·할인 등) | `XxxPolicy` |

예시 (fishlog 예정 도메인):

```
domain
├─ user                     # 회원/인증(로그인 주체)
│  ├─ controller/UserController.java
│  ├─ service/UserService.java
│  ├─ repository/UserRepository.java
│  ├─ entity/User.java  entity/Role.java
│  ├─ dto/UserProfileResponse.java
│  └─ exception/UserErrorCode.java
├─ spot                     # 낚시 스팟 (좌표·주변 검색 → docs/geo.md)
├─ fish                     # 어종 정보
├─ collection               # 어종 도감·사진 인증 (게이미피케이션 → docs/media.md)
└─ tour                     # 주변 관광 시설 (외부 연동 → docs/external.md)
```

**레이어 규칙**
- 의존 방향은 `controller → service → repository` 단방향입니다. 컨트롤러가 리포지토리를 직접 호출하지 않습니다.
- `service`는 **단일 구체 클래스**(`@Service`)로 둡니다. 인터페이스+`Impl`로 분리하지 않으며, 컨트롤러·타 서비스는 이 클래스에 직접 의존합니다.
- 도메인 간 호출이 필요하면 상대 도메인의 **service 클래스**를 통해서만 접근하고, 상대 도메인의 `repository`·`entity` 내부에 직접 접근하지 않습니다.
- DTO는 도메인 경계 안에서만 사용하고, 엔티티를 컨트롤러 응답으로 그대로 노출하지 않습니다(항상 `XxxResponse`로 변환).

### 컨트롤러 Swagger 문서화 규칙 (`XxxControllerSpec`) ✅

프론트 협업용 API 문서(Swagger UI)를 위해, **모든 도메인 컨트롤러는 Swagger 애너테이션을 담는 인터페이스 `XxxControllerSpec`을 만들고 `XxxController`가 이를 `implements`** 한다. 목적은 **컨트롤러 본문에서 문서 애너테이션을 걷어내 로직만 남기고, 방대한 `@Operation`/`@ApiResponse` 문서는 인터페이스에 모으는 것**이다.

**배치·네이밍**
- 같은 `domain/{name}/controller` 패키지에 둔다. 인터페이스 `XxxControllerSpec`, 구현 `XxxController`.
- 파일은 쌍으로 존재: `SpotController.java` + `SpotControllerSpec.java`.

**역할 분리 (무엇을 어디에 두나)**
- **인터페이스(`XxxControllerSpec`)** = 문서 전용. `@Tag`(인터페이스 상단), 메서드마다 `@Operation`·`@ApiResponses`/`@ApiResponse`·`@Content`·`@ExampleObject`·파라미터 `@Schema`. **매핑/바인딩 애너테이션은 두지 않는다.**
- **컨트롤러(`XxxController`)** = 실행 전용. `@RestController`·`@RequestMapping`·`@GetMapping` 등 매핑과 `@PathVariable`/`@RequestParam`/`@RequestBody`/`@Valid` 바인딩, 그리고 `@Override`만. **Swagger 애너테이션을 중복으로 달지 않는다**(springdoc이 인터페이스 애너테이션을 상속 병합).

**반환 타입 = 공통 래퍼 `BaseResponse<T>` (중요)**
- 컨트롤러가 `BaseResponse<XxxResponse>`를 반환하므로, **인터페이스 메서드 시그니처도 `BaseResponse<XxxResponse>`** 로 선언해야 구현이 맞는다(원시 DTO 반환 금지 — 시그니처 불일치).
- 따라서 `@ExampleObject`의 예시 JSON도 **반드시 `BaseResponse` 래핑 형태**로 쓴다. 성공/실패 예시는 이 프로젝트의 실제 응답(`BaseResponse` + `GlobalExceptionHandler`)과 **정확히 일치**해야 한다. 다른 프로젝트에서 흔한 `{ "timestamp", "code", "message" }` 형태를 그대로 복사하지 말 것.
  - **성공:** `{ "success": true, "code": 200, "message": "...", "data": { ... } }`
  - **실패:** `{ "success": false, "code": <status>, "message": "...", "data": null }` (요청 빈도 제한 `429`만 `data`에 `{ "retryAfterSec": n }` 포함)

**description 작성 규칙**
- `@Operation(description = """ ... """)` 텍스트 블록으로 작성하고 `### 설명` / `### 제약조건` / `### ⚠ 예외상황` 섹션을 둔다.
- 예외는 도메인 에러코드(`XxxErrorCode`)를 **`CODE(status)`** 형식으로 나열(예: `SPOT_NOT_FOUND(404)`). `docs/spec.md`·`docs/security.md`의 오류 코드와 일치시킨다.
- multipart(파일 업로드) 엔드포인트는 FormData 전송 구조(키 이름·JSON part)를 description에 명시한다.

**보호(인증) 엔드포인트**
- `@Operation(security = @SecurityRequirement(name = "JWT"))`를 붙인다. 이 이름은 `global/config`의 Swagger 설정(`SwaggerConfig`)에 등록할 **Bearer JWT SecurityScheme 이름과 일치**해야 한다(📋 SwaggerConfig에 스킴 등록 필요 → `docs/security.md`).

**템플릿 (스팟 도메인 예)**

```java
// domain/spot/controller/SpotControllerSpec.java  ← 문서 전용
@Tag(name = "Spot API", description = "낚시 스팟 관련 API")
public interface SpotControllerSpec {

  @Operation(
      summary = "스팟 상세 조회",
      description = """
          ### 설명
          - 스팟 기본정보 + 실시간 예보를 병합해 반환합니다.

          ### 제약조건
          - 경로 변수 spotId: 존재하는 스팟이어야 함

          ### ⚠ 예외상황
          - `SPOT_NOT_FOUND(404)`: 스팟이 존재하지 않는 경우
          """)
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          content = @Content(
              schema = @Schema(implementation = SpotDetailResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "success": true,
                    "code": 200,
                    "message": "요청이 성공적으로 처리되었습니다.",
                    "data": { "spotId": 1, "name": "격포항", "lat": 35.61, "lot": 126.46 }
                  }
                  """))),
      @ApiResponse(
          responseCode = "404",
          description = "스팟을 찾을 수 없음",
          content = @Content(examples = @ExampleObject(value = """
                  { "success": false, "code": 404, "message": "스팟을 찾을 수 없습니다.", "data": null }
                  """)))
  })
  BaseResponse<SpotDetailResponse> getSpotDetail(@Schema(example = "1") Long spotId);
}
```

```java
// domain/spot/controller/SpotController.java  ← 실행 전용
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spots")
public class SpotController implements SpotControllerSpec {

  private final SpotService spotService;

  @Override
  @GetMapping("/{spotId}")
  public BaseResponse<SpotDetailResponse> getSpotDetail(@PathVariable Long spotId) {
    return BaseResponse.success(spotService.getSpotDetail(spotId));
  }
}
```

> `docs/spec.md`가 엔드포인트·Request/Response의 단일 출처이고, `XxxControllerSpec`은 그 명세를 **코드로 구현한 Swagger 문서**다. 엔드포인트 추가/변경 시 둘을 함께 갱신한다.

### 공통 패키지 규칙 (`global/…`)

횡단 관심사를 성격별로 나눕니다. 실제로 도입한 것만 두고, 미도입 항목은 의존성 추가 후 만듭니다.

| 하위 패키지 | 용도 | 상태 |
|---|---|---|
| `common` | 공통 상위 엔티티 등(`BaseTimeEntity`) | ✅ |
| `response` | 공통 응답 래퍼(`BaseResponse`) | ✅ |
| `exception` (+`model`) | 전역 예외 처리·공통 에러 코드(`GlobalExceptionHandler`, `GlobalErrorCode`, `model/BaseErrorCode`) | ✅ |
| `config` | Spring `@Configuration` 모음 — `AsyncConfig`·`PasswordConfig`(BCrypt)·`RedisConfig` 존재. (Swagger 설정은 미도입) | ✅ |
| `security` | Spring Security 설정·인증 진입점·`UserDetails` (`SecurityConfig`, `CustomUserDetails(Service)`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`) → docs/security.md | ✅ |
| `jwt` | JWT 발급·검증(`JwtProvider`)·인증 필터(`JwtAuthenticationFilter`) | ✅ |
| `s3` | S3 업로드 서비스·경로·에러 코드 (docs/media.md) | 📋 |
| `init` | 시드/초기 데이터 로더(`SeedDataInitializer`·`SpotSeedLoader`·`SeedDataReader`) — 스팟/어종 시드 | ✅ |
| `validator` | 커스텀 Bean Validation 애너테이션·검증기 | 📋 |
| `{외부연동}` | 외부 시스템 클라이언트(지도·관광·SMS 등)를 관심사별 하위 패키지로 분리 (docs/external.md) | 📋 |

- `global` 하위에도 서비스가 있으면 도메인과 동일하게 **단일 구체 클래스**로 둡니다(예: `s3/S3Service`).
- 특정 관심사가 여러 클래스(설정·DTO·에러 코드·웹훅 등)로 커지면, 해당 관심사 이름의 하위 패키지로 묶어 응집도를 유지합니다.

### 새 도메인 추가 체크리스트

1. `domain/{name}` 아래 `controller`·`service`·`dto`·`exception`을 기본 생성(엔티티가 필요하면 `entity`·`repository` 추가).
2. `service`는 단일 구체 클래스(`@Service`)로 만든다. `controller`는 `XxxController` + Swagger 문서 인터페이스 `XxxControllerSpec`을 함께 만든다(위 "컨트롤러 Swagger 문서화 규칙").
3. `exception/{Name}ErrorCode`(enum)를 `BaseErrorCode` 구현으로 만들고 **도메인 접두사 코드**(예 `U001`, `S001`)를 부여한다.
4. 엔티티는 `BaseTimeEntity`를 상속한다(`docs/conventions.md` "엔티티 공통 규칙").
5. 응답은 `BaseResponse.success(...)`로 감싸고, 실패는 예외를 던져 `GlobalExceptionHandler`가 변환하게 한다.

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
| `NoHandlerFoundException` / `NoResourceFoundException` | 404 | 매핑 없는 경로·정적 리소스 미존재 |
| `AuthenticationException` | 401 | 미인증·토큰 무효 (Spring Security) |
| `AccessDeniedException` | 403 | 권한 부족 (Spring Security) |
| `SQLTransientConnectionException` | 503 | DB 커넥션 풀 타임아웃 |
| `RedisConnectionFailureException` | 503 | Redis 연결 실패 |
| `Exception` (fallback) | 500 | 예상치 못한 서버 오류 |

> Spring Security(`AuthenticationException` 401 / `AccessDeniedException` 403)·Redis(`RedisConnectionFailureException` 503) 핸들러는 **구현 완료**(해당 의존성 도입됨).

## 공통 엔티티 / Auditing ✅

- **모든 `@Entity`는 `global/common/BaseTimeEntity`를 상속**합니다 → `createdAt`, `modifiedAt` 자동 기록.
- `BaseTimeEntity`: `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)` + `@SuperBuilder`.
- 시각 자동 주입은 `FishlogBeApplication`의 `@EnableJpaAuditing`으로 활성화됨.
- 엔티티 작성 규칙(상속·`@SuperBuilder`·생성자)은 `docs/conventions.md`의 "엔티티 공통 규칙" 참고.

## 설정(Profile) 구성 ✅/📋

- Profile: `local`, `prod` (properties는 `be_config` 서브모듈, `docs/setup.md` 참고).
- 테스트는 DB 없이 실행 (`src/test/resources/application.yml`에서 DataSource/Hibernate 자동설정 제외).

> 위 구조가 확정되면 배지를 ✅로 갱신하고, 실제 패키지 트리와 동기화하세요.