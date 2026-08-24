"""spot_master.json → 시드 로더용 JSON 2종을 생성한다.

입력  : data/spot/spot_master.json   (확정 데이터셋 — 담수 50 + 바다 49 = 99곳, 어종 24종)
출력  : data/spot/spots_seed.json          → SpotSeedData  (spots 테이블 시드)
        data/spot/spot_fish_seed.json      → SpotFishSeedData (major_fish 매핑 시드)
        data/spot/inland_detail_seed.json  → InlandDetailSeedData (inland_spot_detail 시드)

주의 — 실측 상세가 없는 담수 스팟은 제외한다:
  담수 스팟의 상세(하폭·유수폭·수심)는 국립생태원 전국자연환경조사 담수어류 조사기록에서
  왔는데, 조사에서 빠졌거나(조사기록 0건) 어류만 조사되고 하천 제원이 기록되지 않은 곳이
  6곳 있다. 이 스팟들은 상세 정보를 영영 채울 수 없어 **서비스에서 제외**한다
  (spot_master.json 의 detail 이 null 인 담수 스팟 = 제외 대상).

  제외는 시드 생성 단계에서만 일어나고 원본(spot_master.json)에는 조사 결과 그대로
  남겨 둔다. 제외된 스팟은 시드에 없으므로 SpotSeedLoader 의 정리(prune) 단계가
  이미 적재된 DB 행도 major_fish 매핑과 함께 지운다.

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

주의 — 바다 스팟 어종 수 상한(MARINE_FISH_CAP):
  원본의 바다 스팟은 스팟당 평균 8.14종으로 담수(6.48종)보다 과하게 많았다. 해역통계가
  해역 단위로 일괄 배정돼 같은 해역 스팟이 모두 동일한 6~7종을 통째로 받았기 때문이다.
  그 결과 광어는 49곳 전부에 붙고 참돔은 17곳에 그치는 편중이 생겼다.

  그래서 바다 스팟만 어종 수를 MARINE_FISH_CAP 으로 자른다:

    1. 지점실측(바다낚시지수 실측)은 **전량 유지**한다. 그 지점에서 실제 관측된 어종이라
       신뢰도가 가장 높고, 스팟당 최대 6종이라 상한을 넘지 않는다.
    2. 남은 자리는 그 스팟이 속한 해역의 해역통계 어종으로 채우되, **지금까지 가장 적게
       쓰인 어종부터** 고른다(어종별 등장 스팟 수를 고르게 만드는 그리디 균형).
    3. 해역통계 풀이 좁은 해역부터 채운다. 서해는 풀이 3종(갈치·광어·삼치)뿐이라 먼저
       채워야, 풀이 넓은 남해(7종)가 그 3종을 피해 다른 어종을 고를 수 있다.

  서해 스팟은 풀이 3종뿐이라 지점실측이 적으면 상한에 못 미친다(해역 평균 4.42종).
  풀에 없는 어종을 억지로 끼워 넣지 않는다 — 근거 없는 배치를 만들지 않기 위함이다.

  담수 스팟은 전량 지점실측(국립생태원)이라 자르지 않는다.

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
INLAND_DETAIL_OUT = BASE / "inland_detail_seed.json"

SOURCE_NOTE = (
    "data/spot/spot_master.json (담수=국립생태원 지점실측, 바다=바다낚시지수 지점실측 + 해역 어획통계)"
)

INLAND_DETAIL_SOURCE_NOTE = (
    "data/spot/spot_master.json detail (국립생태원 전국자연환경조사 담수어류 — 스팟 반경 약 350m 조사기록 집계, 단위 m)"
)

# 같은 이름의 두 지점을 "같은 장소"로 볼 최대 거리(km).
# 와우천 두 지점이 약 0.7km 라 1.0 이면 병합되고, 나머지 3쌍(6.6/14.8/20.8km)은 분리된다.
DUP_MERGE_KM = 1.0

EARTH_RADIUS_KM = 6371.0

# 바다 스팟 1곳에 배정할 최대 어종 수. 담수 스팟 평균(6.48종)에 맞춘 값이다.
MARINE_FISH_CAP = 6

# 자르기 대상 스팟 category / 유지할 데이터 등급.
MARINE_CATEGORY = "바다"
INLAND_CATEGORY = "담수"
TIER_OBSERVED = "지점실측"
TIER_REGIONAL = "해역통계"

# 담수 스팟 상세(하폭·유수폭·수심) 필드. 최소/최대 쌍이라 병합 시 min/max 로 접는다.
# DETAIL_KEYS 의 순서가 곧 출력 JSON 의 필드 순서다.
DETAIL_KEYS = (
    "riverWidthMin",
    "riverWidthMax",
    "flowWidthMin",
    "flowWidthMax",
    "depthMin",
    "depthMax",
)
DETAIL_MIN_KEYS = frozenset(("riverWidthMin", "flowWidthMin", "depthMin"))


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
            primary["detail"] = merge_details(near)
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


def has_detail(spot: dict) -> bool:
    """담수 상세(하폭·유수폭·수심)가 하나라도 실측된 스팟인지."""
    detail = spot.get("detail")
    return bool(detail) and any(detail.get(k) is not None for k in DETAIL_KEYS)


def merge_details(spots: list[dict]) -> dict | None:
    """같은 장소로 병합된 스팟들의 상세를 하나로 접는다.

    최소는 더 작은 값, 최대는 더 큰 값을 택해 **두 조사지점을 아우르는 범위**로 만든다
    (한쪽만 값이 있으면 그 값이 그대로 남는다). 전부 비면 None.
    """
    details = [s["detail"] for s in spots if has_detail(s)]
    if not details:
        return None
    merged: dict[str, float | None] = {}
    for key in DETAIL_KEYS:
        values = [d[key] for d in details if d.get(key) is not None]
        pick = min if key in DETAIL_MIN_KEYS else max
        merged[key] = pick(values) if values else None
    return merged


def trim_marine_fishes(spots: list[dict]) -> list[str]:
    """바다 스팟의 어종 수를 MARINE_FISH_CAP 이하로 자른다(``spots`` 를 제자리 수정).

    지점실측은 전량 유지하고, 남은 자리만 해역통계 어종으로 채운다. 채울 때는 지금까지
    가장 적게 쓰인 어종을 먼저 골라 어종별 등장 스팟 수를 고르게 만든다. 자세한 근거는
    모듈 docstring "바다 스팟 어종 수 상한" 참고.

    반환: 해역별 요약 로그.
    """
    marine = [s for s in spots if s["category"] == MARINE_CATEGORY]
    if not marine:
        return []

    # 해역(region)별 해역통계 어종 풀. 원본은 같은 해역 스팟에 동일 세트를 배정하므로
    # 합집합을 취해도 해역별 세트가 그대로 나온다.
    region_pool: dict[str, set[str]] = defaultdict(set)
    for spot in marine:
        region_pool[spot["region"]] |= {
            f["name"] for f in spot["fishes"] if f["source"] == TIER_REGIONAL
        }

    # 1단계: 지점실측만 남긴다(스팟당 최대 6종이라 상한을 넘지 않는다).
    picked: dict[int, list[dict]] = {}
    usage: dict[str, int] = defaultdict(int)
    for spot in marine:
        observed = sorted(
            (f for f in spot["fishes"] if f["source"] == TIER_OBSERVED),
            key=lambda f: f["name"],
        )[:MARINE_FISH_CAP]
        picked[id(spot)] = list(observed)
        for fish in observed:
            usage[fish["name"]] += 1

    # 2단계: 해역통계로 빈자리를 채운다. 선택지가 좁은 해역(서해 3종)부터 처리해야
    # 그 해역이 쓸 수밖에 없는 어종을 선택지가 넓은 해역이 피해 갈 수 있다.
    for spot in sorted(marine, key=lambda s: (len(region_pool[s["region"]]), s["name"])):
        chosen = picked[id(spot)]
        candidates = sorted(region_pool[spot["region"]] - {f["name"] for f in chosen})
        while len(chosen) < MARINE_FISH_CAP and candidates:
            # (사용 횟수, 이름) 최소값 → 균형 우선, 동률은 이름으로 갈라 결과를 결정적으로.
            name = min(candidates, key=lambda n: (usage[n], n))
            chosen.append({"name": name, "source": TIER_REGIONAL})
            usage[name] += 1
            candidates.remove(name)
        spot["fishes"] = sorted(chosen, key=lambda f: f["name"])

    by_region: dict[str, list[int]] = defaultdict(list)
    for spot in marine:
        by_region[spot["region"]].append(len(spot["fishes"]))
    logs = [
        f"{region}: {len(sizes)}스팟 평균 {sum(sizes) / len(sizes):.2f}종"
        f" (최소 {min(sizes)}, 해역통계 풀 {len(region_pool[region])}종)"
        for region, sizes in sorted(by_region.items())
    ]
    logs.append(
        "어종별 등장 스팟 수: "
        + ", ".join(f"{n} {c}" for n, c in sorted(usage.items(), key=lambda x: (-x[1], x[0])))
    )
    return logs


def main() -> None:
    master = json.loads(MASTER.read_text(encoding="utf-8"))
    raw_spots = master["spots"]

    # 실측 상세(하폭·유수폭·수심)가 없는 담수 스팟은 서비스에서 제외한다(모듈 docstring 참고).
    usable = []
    excluded = []
    for spot in raw_spots:
        if spot["category"] == INLAND_CATEGORY and not has_detail(spot):
            excluded.append(spot)
        else:
            usable.append(spot)

    kept, merged, split = resolve_duplicate_names(usable)
    # 이름 병합으로 어종이 합집합될 수 있으므로 중복 처리 뒤에 자른다.
    trimmed = trim_marine_fishes(kept)

    # spots_seed.json — 엔티티 필드(name/lat/lot/category). category("바다"/"담수")는
    # Spot.category(해양/내륙)로 적재된다. region·type 은 아직 컬럼이 없어 제외(명세는 docs/spec.md).
    spots_payload = {
        "source": SOURCE_NOTE,
        "spotCount": len(kept),
        "spots": [
            {"name": s["name"], "lat": s["lat"], "lot": s["lng"], "category": s["category"]}
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

    # inland_detail_seed.json — 담수 스팟 상세(하폭·유수폭·수심). 스팟명 기준으로 적재된다.
    inland_details = [
        {"spot": s["name"], **{k: s["detail"][k] for k in DETAIL_KEYS}}
        for s in sorted(kept, key=lambda s: s["name"])
        if s["category"] == INLAND_CATEGORY
    ]
    inland_payload = {
        "source": INLAND_DETAIL_SOURCE_NOTE,
        "unit": "meter",
        "spotCount": len(inland_details),
        "details": inland_details,
    }

    for path, payload in (
        (SPOTS_OUT, spots_payload),
        (SPOT_FISH_OUT, spot_fish_payload),
        (INLAND_DETAIL_OUT, inland_payload),
    ):
        path.write_text(
            json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )

    print(
        f"원본 {len(raw_spots)}곳 → 제외 {len(excluded)}곳 → 채택 {len(kept)}곳"
        f" / 어종 {len(fishes)}종 / 페어 {len(pairs)}개 / 담수 상세 {len(inland_details)}곳"
    )
    for spot in excluded:
        print(f"  [상세 없음 제외] {spot['name']} id={spot['id']} ({spot['type']}, 조사기록 {spot.get('matchedRecords', 0)}건)")
    for line in merged:
        print(f"  [동일 장소 병합] {line}")
    for line in split:
        print(f"  [별개 장소 분리] {line}")
    for line in trimmed:
        print(f"  [바다 어종 상한 {MARINE_FISH_CAP}] {line}")


if __name__ == "__main__":
    main()
