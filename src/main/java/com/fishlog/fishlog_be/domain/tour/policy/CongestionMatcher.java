package com.fishlog.fishlog_be.domain.tour.policy;

import java.util.Map;

/**
 * TourAPI 장소명({@code title}) ↔ 집중률 데이터셋 관광지명({@code tAtsNm}) 매칭 규칙.
 *
 * <p><b>이 클래스가 이 연동의 유일한 실패 지점이다.</b> 두 API는 같은 기관(한국관광공사) 것이지만 공통 ID가 없어서 <b>사람이 쓴 이름</b>으로만 이을 수
 * 있다. 지역코드는 응답에 실려 오므로 기계적으로 해결되는 반면, 이름은 표기가 제각각이라 규칙을 어떻게 잡느냐가 곧 적중률이자 오탐률이다.
 *
 * <p>실측된 표기 차이(2026-09-19, 낚시 스팟 6곳 주변 69건):
 *
 * <pre>
 *   TourAPI title          집중률 tAtsNm                          판단
 *   ─────────────────────────────────────────────────────────────────────
 *   채석강                  채석강 (전북 서해안 국가지질공원)          O  꼬리표만 다름
 *   내소사                  내소사(부안)                            O  괄호 안 지역 표기
 *   속초해수욕장 대관람차     속초해수욕장                            △  상위 장소로 대체 — 허용할지?
 *   궁항리조트              궁항                                    ✗  항구 vs 그 앞 리조트, 다른 곳
 *   신세계센트럴시티 영랑호리조트  영랑호                              ✗  호수 vs 리조트, 다른 곳
 *   격포항                  (없음)                                  -  데이터셋에 항·포구가 거의 없다
 * </pre>
 *
 * <p><b>채택한 규칙: 정규화 후 완전일치.</b> 괄호 구간과 공백만 지우고, 그러고도 이름이 정확히 같을 때만 값을 붙인다.
 *
 * <p>포함관계({@code a.contains(b)})까지 허용하면 적중률이 46% → 57%로 오르지만, 그렇게 얻는 7건이 이런 것들이다(실측 2026-09-19):
 *
 * <pre>
 *   마커 "신세계센트럴시티 영랑호리조트" → 영랑호 82.1(혼잡)    리조트 자리에 옆 호수 수치가 뜬다
 *   마커 "궁항리조트"                  → 궁항 66.8(보통)       숙박시설 자리에 항구 수치가 뜬다
 *   마커 "통영항 여객선터미널"          → 통영항 69.9(보통)  ┐  서로 다른 두 마커에
 *   마커 "통영항 꿀빵거리"              → 통영항 69.9(보통)  ┘  같은 숫자가 찍힌다
 * </pre>
 *
 * <p>혼잡도는 없어도 되는 부가 정보인 반면, <b>틀린 값은 맞는 값과 생김새가 같아서 사용자가 걸러낼 수 없다</b>. 빈칸은 "정보 없음"으로 정확히 읽히지만, 틀린 값
 * 하나는 제대로 맞힌 나머지까지 의심받게 만든다. 그래서 커버리지 11%p를 포기하고 오탐을 구조적으로 만들지 않는 쪽을 택했다. → docs/external.md §2-1
 *
 * <p>다만 완전일치도 원리적 보장은 아니다 — 한 시군구 안에 이름이 같은 다른 장소가 있으면 여전히 틀릴 수 있다(실측 69건에서는 관측되지 않았다).
 */
public final class CongestionMatcher {

  private CongestionMatcher() {}

  /**
   * 장소명에 해당하는 당일 집중률을 찾는다.
   *
   * @param title TourAPI 장소명 (예: {@code 채석강})
   * @param ratesByName 그 시군구의 관광지명 → 집중률 (예: {@code {"채석강 (전북 서해안 국가지질공원)": 39.6, ...}})
   * @return 매칭된 집중률, 없으면 {@code null}(→ 응답의 {@code congestion}이 null이 된다)
   */
  public static Double match(String title, Map<String, Double> ratesByName) {
    if (title == null || title.isBlank() || ratesByName == null || ratesByName.isEmpty()) {
      return null;
    }
    String normalizedTitle = normalize(title);
    if (normalizedTitle.isEmpty()) {
      return null;
    }

    for (Map.Entry<String, Double> entry : ratesByName.entrySet()) {
      if (normalizedTitle.equals(normalize(entry.getKey()))) {
        return entry.getValue();
      }
    }
    return null;
  }

  /**
   * 비교용으로 이름을 정규화한다. 괄호 구간({@code (부안)}, {@code (전북 서해안 국가지질공원)})과 모든 공백을 지운다 — 두 데이터셋이 같은 장소를 두고
   * 실제로 달리 쓰는 부분이 이 둘이다.
   */
  public static String normalize(String name) {
    if (name == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    int depth = 0;
    for (char c : name.toCharArray()) {
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth = Math.max(0, depth - 1);
      } else if (depth == 0 && !Character.isWhitespace(c)) {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
