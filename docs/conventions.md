# conventions.md — 코딩·git·서브모듈 규칙

> 이 문서는 CLAUDE.md에서 항상 자동 로드됩니다. Git 관련 규칙은 저장소 루트 `README.md`가 원본이며, 여기서는 그 내용을 작업 지침 형태로 정리합니다.

## Git 커밋 컨벤션

커밋 메시지는 **이모지 + 타입** 프리픽스를 사용합니다 (README 기준).

| 타입 | 이모지 | 의미 |
|---|---|---|
| Start | 🎉 `:tada:` | 프로젝트 시작 |
| Feat | ✨ `:sparkles:` | 새로운 기능 추가 |
| Fix | 🐛 `:bug:` | 버그 수정 |
| Design | 🎨 `:art:` | UI/디자인 변경 |
| Refactor | ♻️ `:recycle:` | 코드 리팩토링 |
| Settings | 🔧 `:wrench:` | 설정 파일 변경 |
| Comment | 🗃️ `:card_file_box:` | 주석 추가·변경 |
| Dependency/Plugin | ➕ `:heavy_plus_sign:` | 의존성/플러그인 추가 |
| Docs | 📝 `:memo:` | 문서 수정 |
| Merge | 🔀 `:twisted_rightwards_arrows:` | 브랜치 병합 |
| Deploy | 🚀 `:rocket:` | 배포 |
| Rename | 🚚 `:truck:` | 파일/폴더명 변경·이동만 |
| Remove | 🔥 `:fire:` | 파일 삭제만 |
| Revert | ⏪️ `:rewind:` | 이전 버전 롤백 |
| Test | 🧪 `:test_tube:` | 테스트 코드 추가 |

예: `✨ Feat: 어종 도감 인증 API 추가`

## 브랜치 컨벤션 (GitHub Flow)

- `main`: 항상 배포 가능한 상태 유지 (배포 브랜치)
- `develop`: 개발 기본(default) 브랜치
- `feature/{description}`: 기능 개발 브랜치 (예: `feature/social-login`)
- 브랜치 공유 금지 → 특수한 경우 팀원에게 공지
- 팀원 코드는 리뷰 없이 수정 금지 → 수정 시 PR + 리뷰 필수

> **⚠️ 현재 상태 불일치:** README는 default 브랜치를 `develop`으로 명시하지만, 현재 저장소는 `dev` 브랜치를 사용 중입니다. 작업/PR 전에 실제 원격 브랜치명(`git branch -r`)을 확인하고, 팀 규칙(`develop`)과 다르면 사용자에게 확인하세요.

### Flow
1. `develop`에서 새 브랜치 생성
2. 커밋 컨벤션에 맞게 커밋 후 푸시
3. `develop`으로 PR 생성 → 팀 리뷰
4. 리뷰 완료 시 `develop`에 병합
5. 배포 필요 시 `main`으로 PR 생성 → 리뷰
6. 병합 후 배포 (`main` push → CI/CD가 Docker 이미지 빌드·EC2 배포, `docs/setup.md` 참고)

## 서브모듈 컨벤션

설정(`src/main/resources` = `be_config`)은 서브모듈입니다. 수정 시:

1. 서브모듈 수정 → **팀원에게 알리기**
2. 서브모듈 경로로 이동 후 `main`으로 push (브랜치 생성 X)
3. 루트로 나와 해당 브랜치로 push — commit message: `"submodule push"`로 통일
4. `git submodule update --remote`로 최신화
5. 루트에서 바뀐 내용 push — commit message: `"submodule latest"`로 통일
6. 환경변수는 프로젝트별/브랜치별로 분리됨

## 코드 컨벤션

- **포맷터:** Spotless(Google Java Format). 커밋 전 `./gradlew spotlessApply`로 정리. `build` 시 `spotlessCheck`가 함께 실행됨.
- **Lombok:** 사용. (구체 사용 규칙 — `@Getter`/`@Builder`/`@RequiredArgsConstructor` 등 — 📋 팀 합의 후 확정)
- **네이밍/레이어/트랜잭션 경계:** 📋 TBD — `docs/architecture.md`에서 확정해 나감.