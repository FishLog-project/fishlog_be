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

### ⚠️ 알려진 이슈: 배포 서버의 MySQL 포트 노출 (📋 팀 논의 필요)

`docker/docker-compose.yml`의 `db` 서비스가 `ports: - "3307:3306"` 으로 선언돼 있습니다. 포트 매핑에 호스트 IP를 생략하면 **`0.0.0.0`(모든 네트워크 인터페이스)** 에 바인딩됩니다.

- 이 파일은 `deploy.yml`이 **EC2로 SCP**하고, 서버에서 `docker compose up -d api` 실행 시 `depends_on`에 의해 **`db`도 함께 기동**됩니다 → **운영 서버에서 MySQL이 전체 인터페이스에 노출**.
- 현재 실제 접근은 **AWS Security Group**이 막고 있을 가능성이 크지만, 그렇다면 **보안이 SG 설정 하나에만 의존**하는 상태입니다(SG 규칙이 바뀌면 즉시 노출).
- **이 `ports` 블록은 `api` 동작에 불필요합니다.** `api`는 compose 내부 네트워크로 `jdbc:mysql://db:3306`에 접속하며, `ports:`는 **호스트↔컨테이너** 통로일 뿐 컨테이너 간 통신과 무관합니다.

**개선 후보 (택1, 팀 합의 후 적용):**
1. `db`의 `ports` 블록 **제거** — 호스트 노출 0. DB 직접 조회는 `docker exec` 사용.
2. `- "127.0.0.1:3307:3306"` — 서버 자신에서만 접근 가능. SSH 터널로 DB GUI를 쓰는 워크플로가 있다면 이쪽.

> 로컬 개발용 `docker/docker-compose.local.yml`은 앱이 호스트에서(`bootRun`/IDE) 실행되므로 포트 노출이 필요하며, `127.0.0.1:3307:3306`으로 루프백에만 바인딩돼 있습니다. 이 파일은 배포 대상이 아닙니다(`deploy.yml`은 `docker-compose.yml`만 전송).

## Preflight 체크리스트 (작업 시작 전)
1. 서브모듈 최신화 (`git submodule update --remote`).
2. 필수 환경변수/설정 존재 확인 — 없으면 **작업 중단**하고 사용자에게 세팅 요청.
3. 서브모듈 수정 시 서브모듈 커밋과 메인 커밋을 분리.