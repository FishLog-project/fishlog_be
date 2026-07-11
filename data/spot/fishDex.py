"""
바다낚시지수 API (공공데이터포털 15142486) 에서 응답 가능한
대상 어종명(seafsTgfshNm) 목록을 수집·분석하는 스크립트.

spot.py 의 API 호출/페이지네이션/파싱 로직을 그대로 재사용한다.

사용법:
    # data/spot/.env 의 DATA_GO_KR_SERVICE_KEY 를 사용
    python3 data/spot/fishDex.py

결과:
    - 콘솔에 고유 seafsTgfshNm 목록과 개수 출력
    - data/spot/fish_species.json 파일로 저장
"""

from __future__ import annotations

import json
import os
import sys
import time
from collections import Counter
from urllib.error import HTTPError, URLError

import spot  # 같은 폴더의 spot.py 재사용 (fetch_page/extract_items/check_header 등)

OUTPUT_FILE = os.path.join(spot.SCRIPT_DIR, "fish_species.json")


def collect_species(
    service_key: str,
    gubun: str,
    fish_counter: Counter[str],
    fish_gubuns: dict[str, set],
) -> int:
    """한 구분(gubun)의 전체 페이지를 순회하며 seafsTgfshNm 을 집계한다. 수집 레코드 수 반환."""
    total_count: int | None = None
    page_no = 1
    collected = 0

    while True:
        print(f"[fetch] gubun={gubun} page {page_no} ...", file=sys.stderr)
        payload = spot.fetch_page(service_key, gubun, page_no)
        spot.check_header(payload)

        items, page_total = spot.extract_items(payload)
        if total_count is None:
            total_count = page_total
            print(f"[info] gubun={gubun} totalCount = {total_count}", file=sys.stderr)

        if not items:
            break

        for it in items:
            fish = it.get("seafsTgfshNm")
            if fish:
                fish = fish.strip()
                fish_counter[fish] += 1
                fish_gubuns.setdefault(fish, set()).add(gubun)

        collected += len(items)
        if (total_count and collected >= total_count) or len(items) < spot.NUM_OF_ROWS:
            break

        page_no += 1
        time.sleep(0.2)  # 과도한 호출 방지

    return collected


def main() -> int:
    spot.load_dotenv()
    service_key = os.environ.get("DATA_GO_KR_SERVICE_KEY")
    if not service_key or service_key == "여기에_서비스키_입력":
        print(
            "서비스키가 설정되어 있지 않습니다.\n"
            f"  {spot.ENV_FILE} 의 DATA_GO_KR_SERVICE_KEY 값을 채우거나\n"
            '  export DATA_GO_KR_SERVICE_KEY="<서비스키>" 후 다시 실행하세요.',
            file=sys.stderr,
        )
        return 1

    fish_counter: Counter[str] = Counter()
    fish_gubuns: dict[str, set] = {}  # 어종명 -> 등장한 구분 집합
    collected_by_gubun: dict[str, int] = {}

    for gubun in spot.GUBUNS:
        collected_by_gubun[gubun] = collect_species(
            service_key, gubun, fish_counter, fish_gubuns
        )

    collected = sum(collected_by_gubun.values())
    unique_fish = sorted(fish_counter)

    print("\n구분별 수집 레코드:")
    for gubun, cnt in collected_by_gubun.items():
        print(f"  - {gubun}: {cnt}건")
    print(f"총 수집 레코드: {collected}건")
    print(f"고유 seafsTgfshNm(어종) 개수: {len(unique_fish)}개\n")
    for fish in unique_fish:
        gubun_tag = "/".join(sorted(fish_gubuns.get(fish, set())))
        print(f"  - {fish} ({fish_counter[fish]}) [{gubun_tag}]")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {
                "gubuns": spot.GUBUNS,
                "collected_by_gubun": collected_by_gubun,
                "total_records": collected,
                "unique_count": len(unique_fish),
                "species": unique_fish,
                "counts": dict(fish_counter),
                "species_gubuns": {n: sorted(g) for n, g in fish_gubuns.items()},
            },
            f,
            ensure_ascii=False,
            indent=2,
        )
    print(f"\n결과 저장: {OUTPUT_FILE}")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (HTTPError, URLError) as e:
        print(f"[네트워크 오류] {e}", file=sys.stderr)
        sys.exit(1)
    except RuntimeError as e:
        print(f"[오류] {e}", file=sys.stderr)
        sys.exit(1)