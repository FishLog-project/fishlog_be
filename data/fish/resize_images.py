"""도감 이미지 리사이즈 — 서빙용 이미지를 화면 크기에 맞게 줄인다.

원본(1536px)은 도감 그리드에서 200~400px로 그려지므로 4~7배 과하다.
24칸을 동시에 내려받는 화면이라 원본 그대로면 첫 진입에 17MB를 받게 된다.

    py -3 data/fish/resize_images.py                 # 512px로 축소(원본은 자동 백업)
    py -3 data/fish/resize_images.py --dry-run       # 실제 변경 없이 결과만 출력
    py -3 data/fish/resize_images.py --size 768      # 다른 크기로
    py -3 data/fish/resize_images.py --restore       # 백업본으로 되돌리기

기본 동작
  - 대상: data/fish/images/*.png (서버가 서빙하는 폴더 → docs/media.md §0)
  - 원본은 data/fish/images_original/ 로 옮겨 보관한다(.gitignore 처리, 저장소에는 축소본만 커밋).
  - 이미 512px 이하인 파일은 건너뛴다(여러 번 돌려도 화질이 계속 나빠지지 않는다).
  - 알파 채널(RGBA)을 유지한다 — 투명 배경이 사라지면 그리드에 흰 박스가 생긴다.

의존성: Pillow (pip install pillow)
"""

import argparse
import shutil
import sys
from pathlib import Path

# 윈도우 콘솔(cp949)에서 표현 못 하는 문자가 섞여도 스크립트가 죽지 않게 한다.
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(errors="replace")

try:
    from PIL import Image
except ImportError:
    sys.exit("Pillow 가 필요합니다: pip install pillow")

# 이 스크립트(data/fish/) 기준 경로 — 어디서 실행하든 같은 폴더를 본다.
FISH_DIR = Path(__file__).resolve().parent
IMAGE_DIR = FISH_DIR / "images"
BACKUP_DIR = FISH_DIR / "images_original"

DEFAULT_SIZE = 512


def human(num_bytes: int) -> str:
    return f"{num_bytes / 1024:,.0f} KB" if num_bytes < 1024 * 1024 else f"{num_bytes / 1048576:.1f} MB"


def resize_one(path: Path, size: int, backup: bool, dry_run: bool) -> tuple[int, int]:
    """이미지 한 장을 줄인다. (변경 전 바이트, 변경 후 바이트)를 돌려주며, 건너뛰면 둘이 같다."""
    before = path.stat().st_size
    with Image.open(path) as img:
        width, height = img.size
        if max(width, height) <= size:
            print(f"  skip  {path.name:<20} {width}x{height} (이미 {size}px 이하)")
            return before, before
        if dry_run:
            print(f"  plan  {path.name:<20} {width}x{height} -> 최대 {size}px, 현재 {human(before)}")
            return before, before

        # 알파를 보존한다. 원본이 팔레트/그레이스케일이어도 RGBA 로 통일해 투명도가 깨지지 않게 한다.
        converted = img.convert("RGBA")
        # thumbnail: 비율을 유지하며 긴 변을 size 에 맞춘다(정사각형이 아니어도 안전).
        # LANCZOS: 축소 품질이 가장 좋은 리샘플링 필터.
        converted.thumbnail((size, size), Image.Resampling.LANCZOS)
        new_size = converted.size

    if backup:
        BACKUP_DIR.mkdir(exist_ok=True)
        target = BACKUP_DIR / path.name
        if not target.exists():  # 두 번째 실행이 백업본(원본)을 축소본으로 덮어쓰지 않게 한다.
            shutil.copy2(path, target)

    # optimize: 압축을 한 번 더 시도한다(시간은 조금 더 걸리지만 파일이 작아진다).
    converted.save(path, format="PNG", optimize=True)
    after = path.stat().st_size
    print(
        f"  ok    {path.name:<20} {width}x{height} -> {new_size[0]}x{new_size[1]}  "
        f"{human(before)} -> {human(after)}"
    )
    return before, after


def restore() -> None:
    if not BACKUP_DIR.is_dir():
        sys.exit(f"백업 폴더가 없습니다: {BACKUP_DIR}")
    files = sorted(BACKUP_DIR.glob("*.png"))
    for path in files:
        shutil.copy2(path, IMAGE_DIR / path.name)
    print(f"원본 {len(files)}장을 {IMAGE_DIR} 로 되돌렸습니다.")


def main() -> None:
    parser = argparse.ArgumentParser(description="도감 이미지 리사이즈")
    parser.add_argument("--size", type=int, default=DEFAULT_SIZE, help=f"긴 변 최대 픽셀(기본 {DEFAULT_SIZE})")
    parser.add_argument("--dry-run", action="store_true", help="실제로 바꾸지 않고 결과만 출력")
    parser.add_argument("--no-backup", action="store_true", help="원본 백업 생략")
    parser.add_argument("--restore", action="store_true", help="백업본으로 되돌리고 종료")
    args = parser.parse_args()

    if args.restore:
        restore()
        return

    if not IMAGE_DIR.is_dir():
        sys.exit(f"이미지 폴더가 없습니다: {IMAGE_DIR}")

    files = sorted(IMAGE_DIR.glob("*.png"))
    if not files:
        sys.exit(f"png 파일이 없습니다: {IMAGE_DIR}")

    print(f"대상 {len(files)}장 · 최대 {args.size}px · 폴더 {IMAGE_DIR}")
    if not args.dry_run and not args.no_backup:
        print(f"원본 백업 → {BACKUP_DIR}")
    print()

    total_before = total_after = 0
    for path in files:
        before, after = resize_one(path, args.size, not args.no_backup, args.dry_run)
        total_before += before
        total_after += after

    print()
    print(f"합계: {human(total_before)} -> {human(total_after)}", end="")
    if total_before:
        print(f"  ({100 - total_after * 100 / total_before:.0f}% 감소)")
    else:
        print()
    if not args.dry_run:
        print("[주의] 서버는 기동 시 폴더를 스캔하므로 앱 재시작이 필요합니다.")
        print("[주의] 파일명이 같으므로 브라우저 캐시(30일)에 옛 이미지가 남을 수 있습니다 -> 강력 새로고침.")


if __name__ == "__main__":
    main()
