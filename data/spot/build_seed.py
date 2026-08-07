"""spot_master.json → 시드 로더용 JSON 2종을 생성한다.

입력  : data/spot/spot_master.json   (확정 데이터셋 — 담수 50 + 바다 49 = 99곳, 어종 24종)
출력  : data/spot/spots_seed.json      → SpotSeedData  (spots 테이블 시드)
        data/spot/spot_fish_seed.json  → SpotFishSeedData (major_fish 매핑 시드)

주의 — 이름 중복 처리(거리 기준 분리/병합):
  Spot.name 에 UNIQUE 제약이 있고 SpotSeedLoader 가 name 으로 upsert 하므로,
  이름이 겹치는 스팟(위천·청미천·와우천·만경강 4쌍)을 그대로 둘 수 없다.

  예전에는 id 가 작은 쪽만 남기고 나머지를 버렸는데, 그러면 **같은 강 이름을 쓸 뿐
  실제로는 15~20km 떨어진 별개 지점**(위천·청미천)의 조사 데이터가 통째로 사라졌다.
  지금은 두 지점 사이 거리로 갈라 처리한다:

    - DUP_MERGE_KM 이내  → 같은 장소로 보고 **병합**(첫 스팟에 어종 목록을 합집합).
    - DUP_MERGE_KM 초과  → 별개 장소이므로 **이름에 순번을 붙여 분리**(예: 위천(1), 위천(2)).

  순번을 괄호로 감싸는 이유: 원본에 이미 조사지점 번호를 붙인 이름(사정천3·의신천2 등)이
  있어, 접미사 숫자를 그대로 붙이면 조사지점 번호와 구별되지 않는다.

사용: py -3 data/spot/build_seed.py
"""

import json
import math
import pathlib
from collections import defaultdict

BASE = pathlib.Path(__file__).resolve().parent
MASTER = BASE / "spot_master.json"
SPOTS_OUT = BASE / "spots_seed.json"
SPOT_FISH_OUT = BASE / "spot_fish_seed.json"

SOURCE_NOTE = (
    "data/spot/spot_master.json (담수=국립생태원 지점실측, 바다=바다낚시지수 지점실측 + 해역 어획통계)"
)

# 같은 이름의 두 지점을 "같은 장소"로 볼 최대 거리(km).
# 와우천 두 지점이 약 0.7km 라 1.0 이면 병합되고, 나머지 3쌍(6.6/14.8/20.8km)은 분리된다.
DUP_MERGE_KM = 1.0

EARTH_RADIUS_KM = 6371.0


def haversine_km(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """두 좌표 사이 대권 거리(km)."""
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lng2 - lng1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * EARTH_RADIUS_KM * math.asin(math.sqrt(a))


def resolve_duplicate_names(raw_spots: list[dict]) -> tuple[list[dict], list[str], list[str]]:
    """이름이 겹치는 스팟을 거리 기준으로 병합하거나 순번을 붙여 분리한다.

    반환: (채택 스팟 목록, 병합 로그, 분리 로그). 채택 스팟의 ``name`` 은 분리된 경우
    ``원래이름(n)`` 으로 바뀌어 있으므로 이후 단계는 이 값을 그대로 쓰면 된다.
    """
    by_name: dict[str, list[dict]] = defaultdict(list)
    for spot in sorted(raw_spots, key=lambda s: s["id"]):
        by_name[spot["name"]].append(spot)

    kept: list[dict] = []
    merged: list[str] = []
    split: list[str] = []

    for name, group in by_name.items():
        if len(group) == 1:
            kept.append(dict(group[0]))
            continue

        # 첫 스팟을 기준으로 "같은 장소" 묶음(near)과 "별개 장소"(far)를 가른다.
        base = group[0]
        near = [base]
        far = []
        for other in group[1:]:
            km = haversine_km(base["lat"], base["lng"], other["lat"], other["lng"])
            (near if km <= DUP_MERGE_KM else far).append(other)

        # 같은 장소로 판정된 것들은 어종을 합집합해 하나로 만든다(이름·좌표는 첫 스팟 기준).
        primary = dict(base)
        if len(near) > 1:
            primary["fishes"] = union_fishes(near)
            for other in near[1:]:
                km = haversine_km(base["lat"], base["lng"], other["lat"], other["lng"])
                merged.append(f"{name} id={other['id']} → id={base['id']} (거리 {km:.2f}km, 어종 합집합)")

        if not far:
            kept.append(primary)
            continue

        # 별개 장소가 있으면 기준 스팟까지 포함해 전부 순번을 붙인다.
        # (기준만 원래 이름을 유지하면 어느 지점인지 구분되지 않으므로 전부 붙인다.)
        for idx, spot in enumerate([primary] + far, start=1):
            renamed = dict(spot)
            renamed["name"] = f"{name}({idx})"
            kept.append(renamed)
            km = haversine_km(base["lat"], base["lng"], spot["lat"], spot["lng"])
            split.append(f"{renamed['name']} id={spot['id']} ({spot['lat']}, {spot['lng']}) 기준점 거리 {km:.2f}km")

    return kept, merged, split


def union_fishes(spots: list[dict]) -> list[dict]:
    """여러 스팟의 어종 목록을 이름 기준 합집합으로 만든다(먼저 나온 source 를 유지)."""
    by_fish_name: dict[str, dict] = {}
    for spot in spots:
        for fish in spot["fishes"]:
            by_fish_name.setdefault(fish["name"], fish)
    return [by_fish_name[n] for n in sorted(by_fish_name)]


def main() -> None:
    master = json.loads(MASTER.read_text(encoding="utf-8"))
    raw_spots = master["spots"]

    kept, merged, split = resolve_duplicate_names(raw_spots)

    # spots_seed.json — 엔티티 필드(name/lat/lot)만. category·region·type 은
    # Spot 엔티티에 컬럼이 없어 적재하지 않는다(명세는 docs/spec.md 참고).
    spots_payload = {
        "source": SOURCE_NOTE,
        "spotCount": len(kept),
        "spots": [
            {"name": s["name"], "lat": s["lat"], "lot": s["lng"]}
            for s in sorted(kept, key=lambda s: s["name"])
        ],
    }

    # spot_fish_seed.json — 어종 카탈로그 + (스팟, 어종) 페어.
    fishes = sorted({f["name"] for s in kept for f in s["fishes"]})
    pairs = [
        {"spot": s["name"], "fish": f["name"]}
        for s in sorted(kept, key=lambda s: s["name"])
        for f in sorted(s["fishes"], key=lambda f: f["name"])
    ]
    spot_fish_payload = {
        "source": SOURCE_NOTE,
        "spotCount": len(kept),
        "fishCount": len(fishes),
        "pairCount": len(pairs),
        "fishes": fishes,
        "pairs": pairs,
    }

    for path, payload in ((SPOTS_OUT, spots_payload), (SPOT_FISH_OUT, spot_fish_payload)):
        path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )

    print(f"원본 {len(raw_spots)}곳 → 채택 {len(kept)}곳 / 어종 {len(fishes)}종 / 페어 {len(pairs)}개")
    for line in merged:
        print(f"  [동일 장소 병합] {line}")
    for line in split:
        print(f"  [별개 장소 분리] {line}")


if __name__ == "__main__":
    main()
