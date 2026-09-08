package com.fishlog.fishlog_be.domain.collection.policy;

import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import com.fishlog.fishlog_be.domain.collection.exception.CollectionErrorCode;
import com.fishlog.fishlog_be.global.exception.CustomException;

/**
 * 잡은 기록의 공통 규칙 — 수기 입력값(크기·위치·어종명·서식지) 검증·정규화와 사진 표시 상한.
 *
 * <p>도감 인증({@code /verify})과 도감 외 어종({@code /custom})은 저장하는 테이블이 다를 뿐 <b>사용자에게는 같은 화면의 같은 입력란이고 같은
 * 썸네일 그리드</b>다. 규칙을 각 서비스에 복사해 두면 한쪽만 바뀌어 "도감 어종은 300cm까지 되는데 기타 어종은 200cm에서 막힌다", "여긴 사진이 4장인데 저긴
 * 6장 온다" 같은 설명 불가능한 차이가 생기므로, 두 흐름이 이 클래스 하나를 함께 본다.
 */
public final class CatchRecordPolicy {

  /** 기록 가능한 어종 크기 상한(cm). 국내 대상어 최대치(대형 방어·갈치)를 크게 웃도는 값으로, 오타·장난 입력이 크기 랭킹을 장악하는 것을 막는다. */
  public static final double MAX_SIZE_CM = 300.0;

  /**
   * 조회 응답에 담는 최근 사진 수.
   *
   * <p>화면이 썸네일 4칸을 깔고 누르면 오버레이로 키우는 구조라 그 이상은 어차피 그리지 않는다. 기록이 수백 건인 어종에서 응답이 무한정 커지는 것도 막는다. <b>잡은
   * 횟수({@code catchCount})는 이 값으로 자르지 않는다</b> — 두 값의 차이가 "+N장 더"를 만든다.
   */
  public static final int RECENT_PHOTO_LIMIT = 4;

  private CatchRecordPolicy() {}

  /**
   * 크기(cm)를 검증한다.
   *
   * @throws CustomException 없거나 0 이하면 {@code C001}, 상한 초과면 {@code C002}
   */
  public static void validateSize(Double size) {
    if (size == null || size <= 0) {
      throw new CustomException(CollectionErrorCode.INVALID_SIZE);
    }
    if (size > MAX_SIZE_CM) {
      throw new CustomException(CollectionErrorCode.SIZE_OUT_OF_RANGE);
    }
  }

  /**
   * 수기 입력 위치를 저장 형태로 정규화한다. 앞뒤 공백을 제거하고, 미입력·공백만 입력은 모두 {@code null}로 모은다 — "빈 문자열"과 "미입력"이 섞이면 조회
   * 쪽에서 두 가지 빈 값을 각각 처리해야 하기 때문이다.
   *
   * @throws CustomException 트림 후 길이가 {@link CatchRecord#MAX_LOCATION_LENGTH}를 넘으면 {@code C003}. 컬럼
   *     길이와 같은 상수를 보므로 DB가 잘라내기 전에 400으로 걸러진다
   */
  public static String normalizeLocation(String location) {
    if (location == null || location.isBlank()) {
      return null;
    }
    String trimmed = location.trim();
    if (trimmed.length() > CatchRecord.MAX_LOCATION_LENGTH) {
      throw new CustomException(CollectionErrorCode.LOCATION_TOO_LONG);
    }
    return trimmed;
  }

  /**
   * 수기 입력 서식지를 저장 형태로 정규화한다. 위치와 같은 선택 입력 규칙이다 — 앞뒤 공백 제거, 미입력·공백만 입력은 모두 {@code null}.
   *
   * <p>값을 {@code 바다}·{@code 강}·{@code 저수지}·{@code 하천}으로 제한하지 않는다. 도감 어종의 {@code fishes.habitat}도 자유
   * 문자열이라 형태를 맞춘 것이고, 애초에 도감 분류에 안 맞는 어종을 담으려고 만든 기록이라 네 값으로 가두면 적을 말이 없어진다. 후보 제시는 클라이언트 몫이다.
   *
   * @throws CustomException 트림 후 길이가 {@link CustomFish#MAX_HABITAT_LENGTH}를 넘으면 {@code C007}
   */
  public static String normalizeHabitat(String habitat) {
    if (habitat == null || habitat.isBlank()) {
      return null;
    }
    String trimmed = habitat.trim();
    if (trimmed.length() > CustomFish.MAX_HABITAT_LENGTH) {
      throw new CustomException(CollectionErrorCode.HABITAT_TOO_LONG);
    }
    return trimmed;
  }

  /**
   * 수기 입력 어종명을 저장 형태로 정규화한다. 위치와 달리 <b>필수</b>라 공백만 입력하면 {@code null}로 넘기지 않고 거부한다 — 이름 없는 기록은 나중에
   * 사용자도 그게 무슨 물고기였는지 알 수 없다.
   *
   * @throws CustomException 비어 있으면 {@code C004}, {@link CustomFish#MAX_NAME_LENGTH} 초과면 {@code
   *     C005}
   */
  public static String normalizeFishName(String fishName) {
    if (fishName == null || fishName.isBlank()) {
      throw new CustomException(CollectionErrorCode.INVALID_FISH_NAME);
    }
    String trimmed = fishName.trim();
    if (trimmed.length() > CustomFish.MAX_NAME_LENGTH) {
      throw new CustomException(CollectionErrorCode.FISH_NAME_TOO_LONG);
    }
    return trimmed;
  }
}
