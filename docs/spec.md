# spec.md — API 엔드포인트·모델(ERD)

> API 명세와 DB 모델의 단일 출처. 코드 추가/변경 시 **이 문서를 함께 갱신**합니다 (`docs/conventions.md`). 현재는 스켈레톤 + 초안입니다.

## API 엔드포인트

모든 API는 `/api` 베이스 경로. 응답은 공통 응답 포맷(`docs/architecture.md`) 확정 후 반영.

| 상태 | Method | Path | 설명 | 인증 |
|---|---|---|---|---|
| ✅ | GET | `/api/health` | 헬스 체크 `{"status":"ok"}` | 공개 |
| 📋 | POST | `/api/auth/signup` | 회원가입 | 공개 |
| 📋 | POST | `/api/auth/login` | 로그인(JWT 발급) | 공개 |
| 📋 | GET | `/api/spots` | 낚시 스팟 목록/주변 검색 | 공개 |
| 📋 | GET | `/api/spots/{id}` | 스팟 상세(어종·관광·물때) | 공개 |
| 📋 | GET | `/api/fish` | 어종 목록/도감 기준 데이터 | 공개 |
| 📋 | POST | `/api/collections/verify` | 어종 사진 인증 업로드 | 보호 |
| 📋 | GET | `/api/collections/me` | 내 어종 도감 조회 | 보호 |

> 위 경로는 초안입니다. 도메인 확정 시 Request/Response 스키마와 함께 상세화.

## Request / Response 스키마
📋 TBD — 엔드포인트별로 요청/응답 예시(JSON)와 유효성 규칙을 여기에 기록.

## 데이터 모델 (ERD)
📋 TBD — 초안 엔티티: `User`, `Spot`, `Fish`, `SpotFish`(스팟-어종 N:M), `Collection`(사용자-어종 인증), `TourPlace`.
관계·컬럼·인덱스(특히 스팟 위치 공간 인덱스 → `docs/geo.md`)는 도메인 구현과 함께 확정.

```
User 1 ──< Collection >── 1 Fish
Spot 1 ──< SpotFish >── 1 Fish
Spot 1 ──< TourPlace (주변, 외부 API 캐시 여부 TBD)
```