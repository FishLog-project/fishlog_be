"""
바다낚시지수 API (공공데이터포털 15142486) 에서 낚시 스팟 **시드 데이터**를 수집한다.

DB 적재(→ global/init 의 Java 시드 로더)를 위한 두 산출물을 **한 번의 API 순회**로 생성한다.
불변 정보만 추출하며, 예보성(낚시지수·날씨·물때) 필드는 저장하지 않는다.
→ docs/external.md §1, docs/spec.md "스팟 데이터 설계", docs/geo.md

산출물(JSON 키는 Java record 매핑을 위해 camelCase 로 출력):
    - data/spot/spots_seed.json      : 스팟별 name + lat + lot (spots 테이블 시드)
    - data/spot/spot_fish_seed.json  : (스팟, 어종) 페어 (major_fish 매핑 시드)

spot.py 의 API 호출/페이지네이션/파싱 로직을 그대로 재사용한다.

사용법:
    # data/spot/.env 의 DATA_GO_KR_SERVICE_KEY 를 사용
    python3 data/spot/seed.py
"""

from __future__ import annotations

import json
import os
import sys
import time
from urllib.error import HTTPError, URLError

import spot  # 같은 폴더의 spot.py 재사용 (fetch_page/extract_items/check_header 등)

SPOTS_SEED_FILE = os.path.join(spot.SCRIPT_DIR, "spots_seed.json")
FISH_SEED_FILE = os.path.join(spot.SCRIPT_DIR, "spot_fish_seed.json")

# 같은 스팟이 페이지마다 미세하게 다른 좌표를 줄 경우, 이 오차(도 단위)를 넘으면
# 경고만 남기고 첫 좌표를 채택한다.
COORD_EPS = 1e-4

# 대상어종 필드에서 실제 어종이 아닌 플레이스홀더 값 → major_fish 시드에서 제외.
# "-" 는 "대상어종 없음"(주로 선상 오프셋 지명 스팟). "기타어종"은 API 실제 카테고리라
# 유지하되(주요 대상 어종 표시용), 도감 처리 방침은 상위(#12/#13)에서 확정한다.
EXCLUDED_FISH = {"-", ""}


def collect_gubun(service_key: str, gubun: str, spots: dict) -> int:
    """한 구분(gubun)의 전체 페이지를 순회하며 스팟 좌표·어종을 집계한다.

    spots: seafsPstnNm -> {
        "name": str, "lat": float, "lot": float,
        "fishes": set[str], "gubuns": set[str], "records": int,
    }
    수집 레코드 수를 반환한다.
    """
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
            name = (it.get("seafsPstnNm") or "").strip()
            if not name:
                continue

            entry = spots.setdefault(
                name,
                {
                    "name": name,
                    "lat": None,
                    "lot": None,
                    "fishes": set(),
                    "gubuns": set(),
                    "records": 0,
                },
            )
            entry["records"] += 1
            entry["gubuns"].add(gubun)

            lat, lot = it.get("lat"), it.get("lot")
            if lat is not None and lot is not None:
                lat, lot = float(lat), float(lot)
                if entry["lat"] is None:
                    entry["lat"], entry["lot"] = lat, lot
                elif (
                    abs(entry["lat"] - lat) > COORD_EPS
                    or abs(entry["lot"] - lot) > COORD_EPS
                ):
                    print(
                        f"[warn] 좌표 불일치: {name} "
                        f"({entry['lat']},{entry['lot']}) vs ({lat},{lot}) "
                        f"→ 첫 좌표 유지",
                        file=sys.stderr,
                    )

            fish = (it.get("seafsTgfshNm") or "").strip()
            if fish and fish not in EXCLUDED_FISH:
                entry["fishes"].add(fish)

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

    spots: dict[str, dict] = {}
    collected_by_gubun: dict[str, int] = {}
    for gubun in spot.GUBUNS:
        collected_by_gubun[gubun] = collect_gubun(service_key, gubun, spots)

    collected = sum(collected_by_gubun.values())
    ordered = sorted(spots.values(), key=lambda e: e["name"])

    # 좌표 누락 스팟 점검 (있으면 안 됨)
    missing = [e["name"] for e in ordered if e["lat"] is None or e["lot"] is None]
    if missing:
        print(f"[warn] 좌표 누락 스팟 {len(missing)}개: {missing}", file=sys.stderr)

    # --- spots_seed.json : 불변 스팟 정보 (name/lat/lot). prohibit 는 운영값이라 미포함 ---
    spots_payload = [
        {"name": e["name"], "lat": e["lat"], "lot": e["lot"]} for e in ordered
    ]
    with open(SPOTS_SEED_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {
                "source": "data.go.kr 15142486 바다낚시지수",
                "totalRecords": collected,
                "spotCount": len(spots_payload),
                "spots": spots_payload,
            },
            f,
            ensure_ascii=False,
            indent=2,
        )

    # --- spot_fish_seed.json : (스팟, 어종) 페어 (major_fish 시드) ---
    pairs = [
        {"spot": e["name"], "fish": fish}
        for e in ordered
        for fish in sorted(e["fishes"])
    ]
    all_fishes = sorted({p["fish"] for p in pairs})
    with open(FISH_SEED_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {
                "source": "data.go.kr 15142486 바다낚시지수 (seafsTgfshNm, 시점 불변)",
                "spotCount": len(ordered),
                "fishCount": len(all_fishes),
                "pairCount": len(pairs),
                "fishes": all_fishes,
                "pairs": pairs,
            },
            f,
            ensure_ascii=False,
            indent=2,
        )

    # --- 요약 출력 ---
    print("\n구분별 수집 레코드:")
    for gubun, cnt in collected_by_gubun.items():
        print(f"  - {gubun}: {cnt}건")
    print(f"총 수집 레코드: {collected}건")
    print(f"고유 스팟: {len(spots_payload)}개")
    print(f"고유 어종: {len(all_fishes)}개 / (스팟,어종) 페어: {len(pairs)}개")
    no_fish = [e["name"] for e in ordered if not e["fishes"]]
    if no_fish:
        print(f"실어종 없는 스팟(대상어종 '-'/제외값만): {len(no_fish)}개 → {no_fish}")
    print(f"\n결과 저장:\n  - {SPOTS_SEED_FILE}\n  - {FISH_SEED_FILE}")
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
