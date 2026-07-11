"""
바다낚시지수 API (공공데이터포털 15142486) 에서 응답 가능한
낚시터 위치명(seafsPstnNm) 목록을 수집·분석하는 스크립트.

- API: 해양수산부 국립해양조사원_바다낚시지수 조회
- Endpoint: https://apis.data.go.kr/1192136/fcstFishingv2/GetFcstFishingApiServicev2
- 전체 응답 약 1,750건 / 페이지당 최대 300건 → 페이지네이션으로 전량 수집

사용법:
    export DATA_GO_KR_SERVICE_KEY="<공공데이터포털에서 발급받은 서비스키(Decoding 키 권장)>"
    python3 data/spot/spot.py

결과:
    - 콘솔에 고유 seafsPstnNm 목록과 개수 출력
    - data/spot/seafs_pstn_names.json 파일로 저장
"""

from __future__ import annotations

import json
import os
import sys
import time
from collections import Counter
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

# 실제 호출용 엔드포인트 (문서 페이지 openapi.do 가 아님)
BASE_URL = "https://apis.data.go.kr/1192136/fcstFishingv2/GetFcstFishingApiServicev2"
NUM_OF_ROWS = 300  # 페이지당 최대 응답 개수
GUBUN = "갯바위"  # 갯바위 / 선상 중 갯바위만 조회
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
ENV_FILE = os.path.join(SCRIPT_DIR, ".env")
OUTPUT_FILE = os.path.join(SCRIPT_DIR, "seafs_pstn_names.json")


def load_dotenv(path: str = ENV_FILE) -> None:
    """같은 폴더의 .env 파일을 읽어 os.environ 에 채운다(이미 있으면 유지).

    python-dotenv 같은 외부 의존성 없이 KEY=VALUE 형식만 간단히 파싱한다.
    """
    if not os.path.exists(path):
        return
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            if key and key not in os.environ:
                os.environ[key] = value


def encode_service_key(service_key: str) -> str:
    """서비스키를 URL 인코딩한다.

    - Decoding 키(원본, '+'·'/'·'==' 포함)는 percent-encoding 이 필요하다.
    - Encoding 키(이미 '%2B' 등으로 인코딩됨)는 '%' 가 들어 있으므로 그대로 사용한다.
    """
    if "%" in service_key:  # 이미 인코딩된 Encoding 키로 판단
        return service_key
    from urllib.parse import quote

    return quote(service_key, safe="")


def fetch_page(service_key: str, page_no: int, num_of_rows: int = NUM_OF_ROWS) -> dict:
    """단일 페이지를 조회해 파싱된 JSON(dict)을 반환한다.

    필수 파라미터: type(json/xml), gubun(갯바위/선상). 여기서는 갯바위만 조회한다.
    """
    # serviceKey 는 이중 인코딩 방지를 위해 직접 인코딩해 붙이고,
    # 나머지 파라미터만 urlencode 한다.
    params = urlencode(
        {
            "type": "json",
            "gubun": GUBUN,
            "pageNo": page_no,
            "numOfRows": num_of_rows,
        }
    )
    url = f"{BASE_URL}?serviceKey={encode_service_key(service_key)}&{params}"

    req = Request(url, headers={"Accept": "application/json"})
    with urlopen(req, timeout=30) as resp:
        raw = resp.read().decode("utf-8")

    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        # 인증 오류 등은 XML 로 내려오는 경우가 있어 원문을 그대로 보여준다.
        raise RuntimeError(f"JSON 파싱 실패. 응답 원문:\n{raw[:1000]}")


def _body(payload: dict) -> dict:
    """응답 구조가 {response:{body}} 이거나 {body} 인 두 경우를 모두 처리한다."""
    if "response" in payload:
        return payload["response"].get("body", {})
    return payload.get("body", {})


def _header(payload: dict) -> dict:
    if "response" in payload:
        return payload["response"].get("header", {})
    return payload.get("header", {})


def extract_items(payload: dict) -> tuple[list[dict], int]:
    """공공데이터포털 표준 응답 구조에서 item 리스트와 totalCount를 추출한다."""
    body = _body(payload)
    total_count = int(body.get("totalCount", 0) or 0)

    items = body.get("items", {})
    # items 가 {"item": [...]} 형태이거나 비어있을 수 있음
    if isinstance(items, dict):
        item = items.get("item", [])
    else:
        item = items or []
    if isinstance(item, dict):  # 응답이 1건이면 리스트가 아닌 dict
        item = [item]
    return item, total_count


def check_header(payload: dict) -> None:
    """응답 헤더의 resultCode 를 확인하고, 오류면 예외를 던진다."""
    header = _header(payload)
    code = header.get("resultCode")
    if code is not None and code not in ("00", "0"):
        msg = header.get("resultMsg", "알 수 없는 오류")
        raise RuntimeError(f"API 오류 (resultCode={code}): {msg}")


def main() -> int:
    load_dotenv()
    service_key = os.environ.get("DATA_GO_KR_SERVICE_KEY")
    if not service_key or service_key == "여기에_서비스키_입력":
        print(
            "서비스키가 설정되어 있지 않습니다.\n"
            f"  {ENV_FILE} 의 DATA_GO_KR_SERVICE_KEY 값을 채우거나\n"
            '  export DATA_GO_KR_SERVICE_KEY="<서비스키>" 후 다시 실행하세요.',
            file=sys.stderr,
        )
        return 1

    name_counter: Counter[str] = Counter()
    total_count: int | None = None
    page_no = 1
    collected = 0

    while True:
        print(f"[fetch] page {page_no} ...", file=sys.stderr)
        payload = fetch_page(service_key, page_no)
        check_header(payload)

        items, page_total = extract_items(payload)
        if total_count is None:
            total_count = page_total
            print(f"[info] totalCount = {total_count}", file=sys.stderr)

        if not items:
            break

        for it in items:
            name = it.get("seafsPstnNm")
            if name:
                name_counter[name.strip()] += 1

        collected += len(items)
        # totalCount 를 다 모았거나, 마지막 페이지(응답 개수 < 요청 개수)면 종료
        if (total_count and collected >= total_count) or len(items) < NUM_OF_ROWS:
            break

        page_no += 1
        time.sleep(0.2)  # 과도한 호출 방지

    unique_names = sorted(name_counter)
    print(f"\n총 수집 레코드: {collected}건")
    print(f"고유 seafsPstnNm 개수: {len(unique_names)}개\n")
    for name in unique_names:
        print(f"  - {name} ({name_counter[name]})")

    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        json.dump(
            {
                "total_records": collected,
                "unique_count": len(unique_names),
                "names": unique_names,
                "counts": dict(name_counter),
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