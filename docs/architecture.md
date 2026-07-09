# architecture.md — 패키지 구조·레이어·공통 패턴

> 이 문서는 CLAUDE.md에서 항상 자동 로드됩니다. 현재 코드는 최소(health 엔드포인트) 상태라, 아래 상당 부분은 **📋 계획(TBD)** 이며 도메인 구현과 함께 확정합니다.

## 현재 구조 ✅

```
com.fishlog.fishlog_be
├─ FishlogBeApplication.java     # @SpringBootApplication 진입점
└─ controller
   └─ HealthController.java      # GET /api/health → {"status":"ok"}
```

- 모든 API는 `/api` 베이스 경로 아래에 둡니다.
- API 문서: SpringDoc OpenAPI → Swagger UI `/swagger-ui.html`.

## 목표 레이어링 📋

도메인별 패키지 + 레이어 분리를 지향합니다 (확정 전 초안):

```
com.fishlog.fishlog_be
├─ global            # 공통: 응답 포맷, 예외, 설정, 보안, 유틸
│  ├─ common         #   BaseResponse 등 공통 응답 래퍼
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

## 공통 응답 포맷 📋

일관된 응답 래퍼(예: `BaseResponse<T>` — status/message/data)를 두는 것을 권장합니다. 구체 스펙은 팀 합의 후 이 문서에 확정하고, 예제는 `docs/spec.md`에 반영합니다.

## 예외 처리 📋

- 전역 `@RestControllerAdvice` + 도메인 에러 코드(enum) 방식을 권장.
- HTTP 상태 매핑·에러 응답 스키마는 공통 응답 포맷과 함께 확정.

## 설정(Profile) 구성 ✅/📋

- Profile: `local`, `prod` (properties는 `be_config` 서브모듈, `docs/setup.md` 참고).
- 테스트는 DB 없이 실행 (`src/test/resources/application.yml`에서 DataSource/Hibernate 자동설정 제외).

> 위 구조가 확정되면 배지를 ✅로 갱신하고, 실제 패키지 트리와 동기화하세요.