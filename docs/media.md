# media.md — 이미지 업로드(S3)

> 이미지의 업로드·저장·검증 흐름. 저장소는 **AWS S3**. **공통 S3 인프라·프로필 이미지·어종 인증 사진 업로드 모두 ✅ 구현됨.**

## 구현 현황 ✅
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
