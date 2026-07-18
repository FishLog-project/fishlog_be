# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

이 파일은 Claude Code가 본 저장소에서 작업할 때 참고할 **인덱스**입니다. 상세 내용은 성격별로 `docs/`에 분리되어 있습니다.

---

## 프로젝트 한 줄 요약

**fishlog**는 전국 낚시 스팟과 그곳에서 잡히는 어종·주변 관광 시설 정보를 함께 제공하는 **관광 + 낚시 레저 통합 플랫폼**입니다. 사용자는 낚시 스팟 기반으로 여행을 계획하고, 잡은 물고기를 **사진으로 인증**해 자신만의 **어종 도감**을 채워 나가는 게임적 요소를 즐깁니다. 본 저장소는 그 **백엔드 서버**입니다. (자세히 → `docs/product.md`)

- **메인(배포) 브랜치:** `main` / **개발 기본 브랜치:** `develop` (※ 현재 저장소는 `dev` 사용 — `docs/conventions.md` 참고)
- **패키지 루트:** `com.fishlog.fishlog_be` / **앱 이름:** `fishlog_be` / **포트:** 8080

## 기술 스택 (요약)

Java 21 · Spring Boot 4.1.0 · MySQL · Spring Data JPA(Hibernate) · **자체 이메일/비밀번호 로그인 + JWT**(계획) · AWS S3(어종 인증 사진, 계획) · SpringDoc OpenAPI 2.6.0 · Lombok · Spotless(코드 포맷)

## 빌드·실행·테스트 (핵심)

```bash
./gradlew bootRun --args='--spring.profiles.active=local'  # 로컬 실행
./gradlew build                                            # 컴파일 + 테스트 + jar
./gradlew build -x test                                    # 테스트 제외 빌드 (CI/CD가 실제로 쓰는 명령)
./gradlew test                                             # 전체 테스트
./gradlew spotlessApply                                    # 코드 포맷 자동 적용
```
> 상세 명령·환경변수·서브모듈·로컬 인프라 → `docs/setup.md`

> **⚠️ 작업 시작 전 필수 (Preflight, 상세 → `docs/setup.md`):**
> 1. 설정 서브모듈(`src/main/resources` = `be_config`, 환경변수/properties)에 원격 변경이 있으면 **최신화 먼저** (`git submodule update --remote`).
> 2. **필수 환경변수/설정이 세팅돼 있지 않으면 작업을 중단**하고, 사용자에게 먼저 세팅을 요청·대기.
> 3. 서브모듈을 수정하면 **서브모듈 커밋과 메인 커밋을 분리**해서 커밋 (`docs/conventions.md`의 Submodule Convention).

---

## 항상 적용 (자동 로드)

@docs/conventions.md
@docs/architecture.md

---

## 문서 라우팅 — 작업 종류에 맞는 문서를 먼저 읽을 것

| 이런 작업을 할 때 | 읽을 문서 |
|---|---|
| 비즈니스 규칙·도메인 동작(낚시 스팟·어종·도감·관광·게이미피케이션) | `docs/product.md` |
| 패키지 구조·레이어·공통 응답·예외 처리 패턴 | `docs/architecture.md` (항상 로드됨) |
| API 엔드포인트·Request/Response·DB 모델(ERD) 구현 | `docs/spec.md` |
| 네이밍·Lombok·트랜잭션·git 커밋·브랜치·서브모듈 규칙 | `docs/conventions.md` (항상 로드됨) |
| 빌드·환경변수·서브모듈·로컬 인프라(MySQL)·셋업 체크리스트 | `docs/setup.md` |
| 인증/인가/JWT/공개·보호 엔드포인트 정책 | `docs/security.md` |
| 낚시 스팟 좌표·주변(반경) 검색·공간 데이터 | `docs/geo.md` |
| 어종 인증 사진 업로드·S3·이미지 정책 | `docs/media.md` |
| 외부 API 연동(관광 TourAPI·날씨/물때/조위·지도) | `docs/external.md` |

* 상태 배지: **✅ 구현됨 / 🚧 진행중 / 📋 계획(TBD)**. 현재 구현된 API는 `domain/fish`의 전체 도감 조회(`GET /api/fish`, `GET /api/fish/{id}`)뿐이며, 나머지 도메인 문서는 대부분 계획 단계입니다. (헬스 엔드포인트는 미구현.)
* 논의사항이 있을 시 작업을 중단하고 사용자와 논의하여 먼저 해결할 것.
