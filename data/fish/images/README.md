# data/fish/images — 도감 이미지 (정적 서빙)

이 폴더의 파일은 서버가 `/images/fish/**` 경로로 그대로 서빙한다(`global/config/ImageResourceConfig`).
URL 조립·존재 여부 판단은 `global/image/FishImageService`가 담당한다. → `docs/media.md`

## 파일명 규칙

| 파일명 | 쓰이는 곳 |
|---|---|
| `{영문어종명}_image.png` | 도감 이미지 — `/api/collections/dex`(잡은 칸)·`/api/fish/{id}`·`/api/banner/seasonal-fish`·`/api/collections/classify` |
| `{영문어종명}_shadow.png` | 그림자(실루엣) — `/api/collections/dex`의 **아직 못 잡은 칸** |
| `basic_image.png` | 도감 외 어종 공통 기본 이미지 — `/api/collections/custom/dex` |

- `{어종명}`은 DB `fishes.name`과 **완전히 같은 문자열**(예: `감성돔_image.png`, `감성돔_shadow.png`). 24종 × 2 + `basic_image` = **49장**.
- **id 가 아니라 이름을 쓰는 이유:** `fishes.id`는 auto-increment 라 DB를 초기화하면 값이 바뀐다. 어종명은 시드에서 오므로 DB를 몇 번 날려도 그대로다.
- 이름이 틀리면 그 어종만 `imageUrl: null`이 된다(다른 어종에 영향 없음). 오타 확인은 `SELECT name FROM fishes;`.
- **확장자는 고정이 아니다.** `png`·`jpg`·`jpeg`·`webp`·`gif`·`svg` 중 아무거나 두면 기동 시 스캔해서 찾아 쓴다.
  단, 같은 이름에 확장자만 다른 파일을 두 개 두지 말 것(경고 후 하나만 사용).
- 파일이 없으면 해당 응답의 `imageUrl`은 `null`이다(에러 아님) — 일부만 채워 넣고 배포해도 동작한다.

## 어종명 확인

```sql
SELECT name FROM fishes ORDER BY id;
```

파일명과 이 값이 정확히 같아야 한다. 앞뒤 공백·자모 분리(NFD)는 서버가 흡수하지만, 표기 차이(`우럭` vs `조피볼락`)는 다른 어종으로 취급된다.

## 크기 정책 — 512px ✅

서빙용 이미지는 **긴 변 512px**로 통일한다(원본 1536px). 도감 그리드는 24칸을 동시에 내려받는 화면이라
원본 그대로면 첫 진입에 17MB를 받게 되는데, 화면에는 200~400px로 그려지므로 512px면 충분하다.
(24.8MB → 3.7MB, 85% 감소)

```bash
py -3 data/fish/resize_images.py            # 512px로 축소(원본은 images_original/ 로 자동 백업)
py -3 data/fish/resize_images.py --dry-run  # 결과만 미리보기
py -3 data/fish/resize_images.py --restore  # 원본으로 되돌리기
```

- 새 이미지를 추가하면 이 스크립트를 한 번 돌린다(이미 512px 이하인 파일은 건너뛰므로 반복 실행해도 안전).
- 원본은 `data/fish/images_original/`에 보관되며 **.gitignore 처리**된다(저장소에는 축소본만 커밋).
- 알파 채널(RGBA)은 유지된다 — 투명 배경이 사라지면 그리드에 흰 박스가 생긴다.

## 반영 방법

파일을 이 폴더에 넣고 커밋하면 끝. Dockerfile 의 `COPY data ./data`가 배포 이미지에 함께 넣는다.
서버는 기동 시 한 번 스캔하므로 **파일을 추가·교체하면 앱 재시작이 필요**하다.
브라우저 캐시는 30일(`Cache-Control: max-age`)이라, 같은 파일명으로 그림을 교체하면 강력 새로고침 전까지 옛 이미지가 보일 수 있다.
