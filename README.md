## 🎯 Git Convention

- 🎉 **Start:** Start New Project [:tada:]
- ✨ **Feat:** 새로운 기능을 추가 [:sparkles:]
- 🐛 **Fix:** 버그 수정 [:bug:]
- 🎨 **Design:** CSS 등 사용자 UI 디자인 변경 [:art:]
- ♻️ **Refactor:** 코드 리팩토링 [:recycle:]
- 🔧 **Settings:** Changing configuration files [:wrench:]
- 🗃️ **Comment:** 필요한 주석 추가 및 변경 [:card_file_box:]
- ➕ **Dependency/Plugin:** Add a dependency/plugin [:heavy_plus_sign:]
- 📝 **Docs:** 문서 수정 [:memo:]
- 🔀 **Merge:** Merge branches [:twisted_rightwards_arrows:]
- 🚀 **Deploy:** Deploying stuff [:rocket:]
- 🚚 **Rename:** 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우 [:truck:]
- 🔥 **Remove:** 파일을 삭제하는 작업만 수행한 경우 [:fire:]
- ⏪️ **Revert:** 전 버전으로 롤백 [:rewind:]
- 🧪 **Test:** 테스트 코드 추가 [:test_tube:]
- 🔨 **Data:** 데이터 수집/파싱 관련 스크립트 추가/수정 [:hammer:]

## 🪴 Branch Convention (GitHub Flow)

- `main`: 배포 가능한 브랜치, 항상 배포 가능한 상태를 유지
- `develop`: default branch로 변경 (code space)
- `feature/{description}`: 새로운 기능을 개발하는 브랜치
    - 예: `feature/social-login`
- 브랜치 공유 X → 특수한 경우 팀원들에게 알리기
- 팀원이 짠 코드 리뷰 없이 수정 X → 수정 시 PR 남기고 리뷰 필수

### 🌊 Flow

1. `develop` 브랜치에서 새로운 브랜치를 생성
2. 작업을 완료하고 커밋 메시지에 맞게 커밋 후 푸시
3. `develop` 으로 병합 시 Pull Request를 생성 / 팀원들의 리뷰
4. 리뷰가 완료되면 `develop` 브랜치로 병합
5. 병합 후, 배포 필요 시 `main` 브랜치로 Pull Request를 생성 / 팀원들의 리뷰 진행
6. 병합 후 배포

## 🔧 Submodule Convention

1. submodule 수정 → 팀원들에게 알리기
2. submodule 경로로 이동 후 `main`으로 push (브랜치 생성 X)
3. 루트 경로로 나와서 해당 브랜치로 push
    - commit message: `"submodule push"`로 통일
4. `git submodule update --remote`로 서브모듈 최신화
5. 루트 경로에서 바뀐 내용 해당 브랜치로 push
    - commit message: `"submodule latest"`로 통일
6. 프로젝트별로 환경변수 브랜치 별로 분리됨
