# setup.md — 빌드·환경변수·서브모듈·로컬 인프라

## 사전 요구사항
- JDK 21 (Temurin 권장 — CI/Docker와 동일)
- MySQL (로컬 개발용)
- Docker (선택: 로컬 인프라/배포 이미지 확인용)

## 서브모듈 (설정 = `be_config`) ⚠️ 필수

`src/main/resources`는 이 저장소가 아니라 **git 서브모듈**입니다 (`.gitmodules` → `FishLog-project/be_config`, branch `main`). 실제 `application-*.properties`와 DB 접속 정보 등이 여기에 있습니다.

```bash
git submodule update --init --recursive   # 최초 클론 후 필수 (없으면 빌드/실행 불가)
git submodule update --remote             # 설정 변경 최신화
```
> 서브모듈 수정·커밋 규칙은 `docs/conventions.md`의 Submodule Convention 참고.

## 빌드·실행·테스트

```bash
./gradlew bootRun --args='--spring.profiles.active=local'  # 로컬 실행 (포트 8080)
./gradlew build                                            # 컴파일 + 테스트 + jar
./gradlew build -x test                                    # 테스트 제외 (CI/CD가 실제로 쓰는 명령)
./gradlew test                                             # 전체 테스트
./gradlew test --tests 'com.fishlog.fishlog_be.FishlogBeApplicationTests'  # 단일 테스트
./gradlew spotlessApply                                    # 코드 포맷 적용
./gradlew spotlessCheck                                    # 포맷 검사 (build에 연결됨)
```

## 프로파일 & 환경변수
- Profile: `local`, `prod`. properties 파일은 서브모듈(`be_config`)에 위치.
- 필요한 환경변수(DB, JWT 시크릿, AWS S3, 외부 API 키 등)는 서브모듈 properties 또는 실행 환경에 주입.
- 📋 **필수 환경변수 목록은 도메인 구현과 함께 이 문서에 표로 정리** (현재 미확정).

## 테스트 환경
- `src/test/resources/application.yml`이 `DataSourceAutoConfiguration`·`HibernateJpaAutoConfiguration`을 제외 → 테스트는 DB 없이 실행.
- JPA/MySQL이 필요한 테스트는 해당 자동설정을 다시 켜거나 테스트용 DataSource를 제공해야 함.

## CI/CD
- `.github/workflows/ci.yml` — `main`/`dev` PR 시 `./gradlew build -x test` (테스트 제외).
- `.github/workflows/deploy.yml` — `main` push 시: 빌드 → Docker Hub 이미지 push(`<user>/fishlog-app:latest`, `:<sha>`) → EC2에 SSH 후 `docker compose pull api && docker compose up -d api`.
- **주의:** 파이프라인은 `-x test`로 **테스트를 건너뜀**. 로컬에서 `./gradlew test`로 검증 후 푸시할 것.
- `Dockerfile`은 `build/libs/fishlog_be-0.0.1-SNAPSHOT.jar`를 복사 → `build.gradle`의 `version` 변경 시 Dockerfile COPY 경로도 함께 수정.

## Preflight 체크리스트 (작업 시작 전)
1. 서브모듈 최신화 (`git submodule update --remote`).
2. 필수 환경변수/설정 존재 확인 — 없으면 **작업 중단**하고 사용자에게 세팅 요청.
3. 서브모듈 수정 시 서브모듈 커밋과 메인 커밋을 분리.