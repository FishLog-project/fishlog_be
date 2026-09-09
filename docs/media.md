# media.md — 이미지(업로드 S3 · 도감 이미지 정적 서빙)

> 이미지는 성격이 둘로 나뉜다.
>
> | 구분 | 예 | 저장소 | 문서 |
> |---|---|---|---|
> | **사용자가 올리는 사진**(가변, 사용자마다 다름) | 프로필·어종 인증·도감 외 어종 사진 | **AWS S3** | 아래 "업로드(S3)" |
> | **도감 마스터 이미지**(불변, 49장 고정) | 어종 이미지·그림자·도감 외 어종 기본 이미지 | **서버 정적 파일(`data/fish/images/`)** | 아래 §0 |
>
> **공통 S3 인프라·프로필 이미지·어종 인증 사진 업로드·도감 이미지 정적 서빙 모두 ✅ 구현됨.**

## §0 도감 이미지 — 서버 정적 서빙 ✅

어종 도감 이미지는 **어종 수만큼만 존재하는 불변 리소스**(24종 × 2 + 기본 1 = **49장**)라 S3에 두지 않고 서버가 직접 서빙한다.

### 구성

| 요소 | 위치 | 역할 |
|---|---|---|
| 이미지 파일 | `data/fish/images/` (프로퍼티 `fishlog.image.dir`) | 실제 파일. 시드 JSON과 같은 `data/` 아래라 Dockerfile의 `COPY data ./data`가 이미 배포한다 |
| `ImageResourceConfig` | `global/config` | `/images/fish/**` → 위 디렉터리 정적 매핑(`WebMvcConfigurer`). 30일 public 캐시 |
| `FishImageService`(+`Impl`) | `global/image` | 어종명 → **절대 URL** 생성. 기동 시 폴더를 1회 스캔해 `감성돔_image → 감성돔_image.png` 맵 구성 |
| `SecurityConfig` | `global/security` | `/images/fish/**`를 GET 공개(로그인 전 화면·`<img src>`에서 받아야 함) |

### 파일명 규칙

| 파일명 | 쓰이는 곳 |
|---|---|
| `{영문어종명}_image.*` | `/api/collections/dex`의 **잡은 칸** · `/api/fish/{id}` · `/api/banner/seasonal-fish` · `/api/collections/classify` 후보 |
| `{영문어종명}_shadow.*` | `/api/collections/dex`의 **아직 못 잡은 칸** |
| `basic_image.*` | `/api/collections/custom/dex`(도감 외 어종) **전 칸 공통** |

`{어종명}`은 `fishes.name`과 완전히 같은 문자열이다(예: `감성돔_image.png`). 이름이 틀리면 **그 어종만** `imageUrl: null`이 되고 나머지는 정상 동작한다.

- **확장자는 고정하지 않는다.** `png`·`jpg`·`jpeg`·`webp`·`gif`·`svg` 중 무엇이든 두면 기동 스캔이 찾아 쓴다. 코드에 확장자를 박으면 그림을 교체할 때마다 코드를 고쳐야 하기 때문이다.
- 파일이 없으면 **예외가 아니라 `imageUrl: null`**. 이미지 큐레이션이 끝나지 않아도 API는 그대로 동작한다(응답 스키마상 nullable).
- 서버는 **기동 시 한 번만** 스캔한다 → 파일 추가·교체 후에는 **앱 재시작 필요**.

### 왜 이 구조인가 (설계 근거)

- **어종 id 가 아니라 어종명을 키로 쓴다 ✅(확정).** `fishes.id`는 auto-increment 라 **DB를 초기화하면 값이 바뀐다**(실제로 시드를 다시 넣으며 파일명과 어긋났다). 반면 `fishes.name`은 시드 파일에서 오므로 DB를 몇 번 날려도 동일하고, `UNIQUE` 제약이 있으며, **AI 모델 서버와의 조인 키로 이미 쓰이고 있다**(→ `docs/external.md` §2). 이미지도 같은 키를 쓰는 것이 일관된다.
- **DB에 URL을 저장하지 않는다.** `fishes.image_url` 컬럼은 더 이상 응답에 쓰이지 않는다(값은 계속 null). id로 규칙 생성하면 시드·마이그레이션이 필요 없고, 로컬(`localhost:8080`)과 배포 도메인이 달라도 URL이 자동으로 맞는다.
- **`src/main/resources`에 두지 않는다.** 그 경로는 설정 서브모듈(`be_config`)이라 이미지를 넣으면 팀 전원이 서브모듈 푸시 절차를 밟아야 한다. → `docs/conventions.md`
- **컨트롤러로 바이트를 스트리밍하지 않는다.** 스프링 `ResourceHttpRequestHandler`가 Content-Type 판별·ETag/If-Modified-Since·Range·경로 이탈(`../`) 차단을 이미 처리한다.
- **그림자를 클라이언트 이펙트로 만들지 않는다.** 실루엣 그림을 따로 준비했으므로, 어느 쪽을 쓸지 아는 서버가 `imageUrl` 하나로 확정해 준다(도감 응답의 `caught`는 완성도·필터용으로 유지). 미획득 어종의 실제 이미지 URL은 응답에 싣지 않아 스포일러도 막는다.
- **도감 외 어종은 사용자 사진이 아니라 기본 이미지.** 그리드는 도감 그리드와 나란히 놓이는 화면이라 통일된 아이콘이 낫고, 본인 사진은 상세(`GET /api/collections/custom?customFishId=`)에서 본다.

### 파일명은 영문 슬러그 — 한글을 파일시스템·URL에 내보내지 않는다 ✅

파일명은 `black_seabream_image.png`처럼 **ASCII 슬러그**다. 한글 어종명(`fishes.name`)은 `global/image/FishImageName` enum 이 슬러그로 바꿔 주며, 한글은 **조회 키로만** 쓰이고 파일시스템·URL에는 닿지 않는다.

**한글 파일명을 쓰지 않는 이유(세 문제가 한꺼번에 사라진다):**

| 문제 | 한글 파일명일 때 | 영문 슬러그일 때 |
|---|---|---|
| URL | `%EA%B0%90%EC%84%B1%EB%8F%94_image.png` 로 퍼센트 인코딩 → **Swagger 문서·로그가 읽기 어려움** | `black_seabream_image.png` 그대로 |
| 파일시스템 | macOS 는 NFD(자모 분리)로 저장 → NFC 인 DB 값과 불일치 | 영향 없음 |
| JVM | 컨테이너 로케일이 C/POSIX(ASCII)면 파일명을 `???` 로 읽어 **배포 환경에서만 전부 null** | 영향 없음 |

- **매핑을 DB 컬럼이 아니라 enum 에 둔 이유:** 영문명은 사용자에게 보여 주는 값도 도메인 규칙도 아니고 "이 어종의 이미지 파일을 뭐라 부르는가"일 뿐이다. 어종 테이블에 화면과 무관한 컬럼이 늘지 않고, 오타가 컴파일 시점에 드러난다.
- 어종명 입력의 **앞뒤 공백·유니코드 정규화(NFD/NFC)** 차이는 enum 조회에서 흡수한다.
- ⚠️ **도감 어종이 늘거나 이름이 바뀌면 `FishImageName` 도 함께 고친다.** 매핑에 없는 어종은 그 어종만 `imageUrl: null`이 되고 나머지에는 영향이 없다.

### 절대 URL 조립

1. `fishlog.image.base-url`이 설정돼 있으면 그 값을 접두사로 쓴다(예 `https://api.fishlog.com`).
2. 없으면 **현재 요청의 컨텍스트 URL**을 쓴다. 배포 compose가 `SERVER_FORWARD_HEADERS_STRATEGY=framework`를 켜 두어 프록시 뒤에서도 외부 기준 스킴·호스트가 잡힌다 → **설정 없이 로컬/배포 모두 동작**.
3. 요청 스레드 밖(스케줄러 등)에서 호출되면 접두사 없이 경로(`/images/fish/...`)만 반환한다.

### 크기 정책 — 512px ✅

서빙용 이미지는 **긴 변 512px**로 통일한다. 도감 그리드가 24칸을 동시에 로드하는 화면이라 원본(1536px, 장당 0.5~1.1MB)을 그대로 두면 **첫 진입에 17.6MB**를 받게 되는데, 실제 표시 크기는 200~400px이라 4~7배 과했다. 축소 후 폴더 전체 **24.8MB → 3.7MB(85% 감소)**, 그리드 24칸 **17.6MB → 2.8MB**.

- 변환 스크립트: `data/fish/resize_images.py` (Pillow, LANCZOS 리샘플링 + PNG optimize).
  - `--dry-run` 미리보기 · `--size N` 다른 크기 · `--restore` 원본 복구.
  - **멱등**하다 — 이미 512px 이하인 파일은 건너뛰므로 반복 실행해도 화질이 계속 나빠지지 않는다.
- 원본은 `data/fish/images_original/`에 백업되고 **.gitignore 처리**된다. 저장소·도커 이미지에는 축소본만 들어간다.
- 변환은 **RGBA를 유지**한다. 알파가 날아가면 투명 배경이 흰 사각형이 되어 그리드가 깨진다.

### 이미지 교체 시 주의

같은 파일명으로 그림만 바꾸면 브라우저가 30일 캐시된 옛 이미지를 계속 쓸 수 있다. 강력 새로고침을 안내하거나 파일명을 바꾼다.

---

## 업로드(S3) — 사용자가 올리는 사진

### 구현 현황 ✅
- **공통 S3 계층 `global/s3`**: `S3Service`(+`Impl`, AWS SDK v2 `S3Client`), `PathName`(경로 prefix enum: `PROFILE`·`FISH`·`CUSTOM_FISH`), `S3ErrorCode`(S001~S004).
  - `upload(MultipartFile, PathName)` — 검증(이미지만·최대 5MB) 후 `{prefix}/{uuid}{ext}`로 업로드, 접근 URL 반환.
  - `delete(url)` — URL에서 key를 파싱해 객체 삭제.
- **`S3Config`**(`global/config`): `spring.cloud.aws.*` 값으로 `S3Client` 빈 직접 구성(리전·정적 자격증명).
- **적용:**
  - 프로필 이미지 `POST /api/users/me/profile-image`(→ `users.profile_image_url`, `PathName.PROFILE`).
  - **어종 인증 사진 `POST /api/collections/verify`**(→ `catch_record.certified_image_url`, `PathName.FISH`). 업로드 후 DB 저장이 실패하면 **S3 객체를 보상 삭제**해 고아 객체를 남기지 않는다. → `docs/spec.md`
  - **도감 외 어종 사진 `POST /api/collections/custom`**(→ `custom_catch_record.certified_image_url`, `PathName.CUSTOM_FISH` = `custom-fish/`). 보상 삭제 패턴은 위와 동일하다.
    - **경로를 `fish/`와 나눈 이유:** 이쪽 사진에는 모델·도감 어느 쪽으로도 검증되지 않은 이름이 붙어 있다. 나중에 신규 어종 후보를 추리거나 학습 데이터로 쓸 때, 검증된 사진과 한 prefix에 섞여 있으면 골라낼 방법이 없다. → `docs/spec.md`
  - → `docs/spec.md`, `docs/security.md`.

## 크기 한도 — 한 곳에서 관리 ✅

- `S3Service.MAX_IMAGE_SIZE`(**5MB**)가 단일 출처다. 저장(S3)뿐 아니라 **어종 분류(AI) 경로도 같은 상수를 검증에 쓴다.**
  - 이유: 분류 한도(모델 계약 10MB)가 저장 한도보다 느슨하면 "분류는 성공했는데 인증 저장이 실패"하는 흐름이 생긴다. 한도를 묶어 **분류에 성공한 사진은 반드시 저장도 가능**하게 맞췄다.
- 컨테이너 한도 `spring.servlet.multipart.max-file-size=10MB`는 일부러 더 느슨하다. 그래야 초과분이 컨테이너에서 잘려 500이 되지 않고, `MaxUploadSizeExceededException` 핸들러가 **413 + 명확한 메시지**로 변환한다.
  - ⚠️ 이 설정 이전에는 Spring Boot 기본값(`1MB`)이 적용돼, 1~5MB 사진은 `S3Service` 검증에 닿기도 전에 잘리고 500으로 나갔다.

## 저장소
- **AWS S3** (배포 환경이 EC2라 AWS 생태계와 정합). **SDK v2 사용**(`software.amazon.awssdk:s3`).
- 버킷·리전·자격증명은 환경변수(`be_config` 서브모듈) 주입 → `docs/setup.md`.
  - `AWS_ACCESS_KEY`/`AWS_SECRET_KEY`(자격증명), `spring.cloud.aws.region.static`(리전), `spring.cloud.aws.s3.bucket`(버킷).
  - ⚠️ IAM 사용자에 `s3:PutObject`·`s3:GetObject`·`s3:DeleteObject` 권한 필요(없으면 403).

## 업로드 흐름
- **서버 경유 업로드 ✅(현재 방식):** `MultipartFile`로 서버가 받아 S3에 업로드. 구현 단순.
- **Presigned URL(향후 대안 📋):** 대용량·서버 부하 우려 시 서버가 presigned PUT URL 발급 → 클라이언트 직접 업로드로 전환 검토.

## 검증·정책 (초안)
- 허용 확장자/용량 제한, 이미지 리사이즈/썸네일 여부. 📋
- 인증 사진과 어종 매칭 ✅(확정): **AI 분류로 후보를 제시하고 어종은 사용자가 확정**한다(수동 승인 없음). → `docs/external.md` §2, `docs/spec.md`
- 부적절 이미지 신고/삭제 흐름. 📋

## 확정 필요 항목
- [ ] 업로드 방식(Presigned vs 서버 경유)
- [x] 용량 제약 — `S3Service.MAX_IMAGE_SIZE` 5MB 단일 출처 ✅ / [ ] 포맷·해상도 제약, 썸네일 생성 여부 📋
- [ ] S3 객체 키 규칙 / 접근 제어(공개 vs presigned GET)
- [x] 인증 사진 ↔ 어종 판별/승인 방식 — AI 후보 제시 + 사용자 확정 ✅
