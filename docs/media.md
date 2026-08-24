# media.md — 이미지 업로드(S3)

> 이미지의 업로드·저장·검증 흐름. 저장소는 **AWS S3**. **공통 S3 인프라·프로필 이미지 업로드는 ✅ 구현됨**, 어종 인증 사진은 📋 계획.

## 구현 현황 ✅
- **공통 S3 계층 `global/s3`**: `S3Service`(+`Impl`, AWS SDK v2 `S3Client`), `PathName`(경로 prefix enum: `PROFILE`·`FISH`), `S3ErrorCode`(S001~S004).
  - `upload(MultipartFile, PathName)` — 검증(이미지만·최대 5MB) 후 `{prefix}/{uuid}{ext}`로 업로드, 접근 URL 반환.
  - `delete(url)` — URL에서 key를 파싱해 객체 삭제.
- **`S3Config`**(`global/config`): `spring.cloud.aws.*` 값으로 `S3Client` 빈 직접 구성(리전·정적 자격증명).
- **적용:** 프로필 이미지 `POST /api/users/me/profile-image`(→ `users.profile_image_url`, `PathName.PROFILE`). → `docs/spec.md`, `docs/security.md`.

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
- 인증 사진과 어종 매칭(수동 승인 vs 자동 판별)은 `docs/product.md`의 인증 승인 규칙과 연동. 📋
- 부적절 이미지 신고/삭제 흐름. 📋

## 확정 필요 항목
- [ ] 업로드 방식(Presigned vs 서버 경유)
- [ ] 이미지 제약(포맷·용량·해상도) 및 썸네일 생성 여부
- [ ] S3 객체 키 규칙 / 접근 제어(공개 vs presigned GET)
- [ ] 인증 사진 ↔ 어종 판별/승인 방식
