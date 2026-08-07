"""spot_master.json → 시드 로더용 JSON 2종을 생성한다.

입력  : data/spot/spot_master.json   (확정 데이터셋 — 담수 50 + 바다 49 = 99곳, 어종 24종)
출력  : data/spot/spots_seed.json      → SpotSeedData  (spots 테이블 시드)
        data/spot/spot_fish_seed.json  → SpotFishSeedData (major_fish 매핑 시드)

주의 — 이름 중복 제거:
  Spot.name 에 UNIQUE 제약이 있고 SpotSeedLoader 가 name 으로 upsert 하므로,
  이름이 겹치는 스팟은 **id 가 작은 쪽 하나만** 남긴다(위천·청미천·와우천·만경강 4쌍).
  결과적으로 99곳 중 95곳이 적재된다. 제외된 스팟은 실행 로그에 출력한다.

사용: py -3 data/spot/build_seed.py
"""

import json
import pathlib

BASE = pathlib.Path(__file__).resolve().parent
MASTER = BASE / "spot_master.json"
SPOTS_OUT = BASE / "spots_seed.json"
SPOT_FISH_OUT = BASE / "spot_fish_seed.json"

SOURCE_NOTE = (
    "data/spot/spot_master.json (담수=국립생태원 지점실측, 바다=바다낚시지수 지점실측 + 해역 어획통계)"
)


def main() -> None:
    master = json.loads(MASTER.read_text(encoding="utf-8"))
    raw_spots = master["spots"]

    # 이름 UNIQUE 제약 대응: 같은 이름은 id 가 작은 첫 번째만 채택.
    kept, dropped, seen = [], [], set()
    for spot in sorted(raw_spots, key=lambda s: s["id"]):
        if spot["name"] in seen:
            dropped.append(spot)
            continue
        seen.add(spot["name"])
        kept.append(spot)

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
    for s in dropped:
        print(f"  [중복 제외] id={s['id']} {s['name']} ({s['lat']}, {s['lng']})")


if __name__ == "__main__":
    main()
