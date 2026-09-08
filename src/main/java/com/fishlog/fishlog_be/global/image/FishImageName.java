package com.fishlog.fishlog_be.global.image;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 도감 어종명(한글) ↔ 이미지 파일명(영문 슬러그) 매핑.
 *
 * <p><b>왜 파일명을 영문으로 두나 ✅(확정):</b> 파일명이 한글이면 URL·파일시스템·JVM 세 군데에서 인코딩 문제가 동시에 생긴다 — URL 퍼센트 인코딩으로
 * Swagger 문서와 로그가 읽기 어려워지고, macOS 는 파일명을 NFD 로 저장해 NFC 인 DB 값과 어긋나며, 컨테이너 로케일이 ASCII 면 JVM 이 파일명을
 * 아예 읽지 못한다. 파일명을 ASCII 로 고정하면 이 셋이 한꺼번에 사라진다.
 *
 * <p><b>왜 DB 컬럼이 아니라 enum 인가:</b> 영문명은 사용자에게 보여 주는 값도, 도메인 규칙도 아니고 <b>"이 어종의 이미지 파일을 뭐라고 부르는가"</b>일
 * 뿐이다. 이미지 모듈 안에 두면 어종 테이블에 화면과 무관한 컬럼이 늘지 않고, 매핑이 코드로 남아 오타가 컴파일 시점에 드러난다.
 *
 * <p>enum 상수 이름을 <b>소문자로 바꾼 것이 곧 슬러그</b>다({@code BLACK_SEABREAM} → {@code black_seabream} → 파일
 * {@code black_seabream_image.png} · {@code black_seabream_shadow.png}).
 *
 * <p>⚠️ <b>도감 어종이 늘거나 이름이 바뀌면 여기도 함께 고친다.</b> 여기에 없는 어종은 이미지가 {@code null}로 나갈 뿐 다른 어종에는 영향이 없다. 어종
 * 목록의 단일 출처는 {@code data/fish/fish_content_seed.json} 이다. → docs/media.md §0
 */
public enum FishImageName {
  // --- 바다 ---
  BLACK_SEABREAM("감성돔"),
  SEABASS("농어"),
  ROCK_BREAM("돌돔"),
  NIBBLER("벵에돔"),
  KOREAN_ROCKFISH("우럭"),
  RED_SEABREAM("참돔"),
  OLIVE_FLOUNDER("광어"),
  DARKBANDED_ROCKFISH("볼락"),
  HAIRTAIL("갈치"),
  MACKEREL("고등어"),
  SPANISH_MACKEREL("삼치"),
  YELLOWTAIL("방어"),
  HORSE_MACKEREL("전갱이"),
  MULLET("숭어"),
  // --- 민물 ---
  CRUCIAN_CARP("붕어"),
  CARP("잉어"),
  MANDARIN_FISH("쏘가리"),
  LARGEMOUTH_BASS("배스"),
  BLUEGILL("블루길"),
  SNAKEHEAD("가물치"),
  CATFISH("메기"),
  TROUT("송어"),
  PALE_CHUB("피라미"),
  KOREAN_BULLHEAD("동자개");

  /** 한글 어종명(NFC) → enum. 조회는 요청마다 일어나므로 미리 만들어 둔다. */
  private static final Map<String, FishImageName> BY_KOREAN_NAME =
      Arrays.stream(values())
          .collect(Collectors.toMap(FishImageName::getKoreanName, Function.identity()));

  /** {@code fishes.name}과 같은 한글 어종명. */
  private final String koreanName;

  FishImageName(String koreanName) {
    this.koreanName = koreanName;
  }

  public String getKoreanName() {
    return koreanName;
  }

  /** 이미지 파일명 앞부분(ASCII 슬러그). 상수 이름을 소문자로 바꾼 값이다. */
  public String getSlug() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * 한글 어종명으로 슬러그를 찾는다. 앞뒤 공백과 유니코드 정규화(NFD/NFC) 차이는 흡수한다.
   *
   * @return 매핑이 없으면 빈 값 — 예외로 만들지 않는다. 도감에 어종을 먼저 추가하고 이미지를 나중에 붙이는 순서가 가능해야 하기 때문이다.
   */
  public static Optional<FishImageName> ofKoreanName(String koreanName) {
    if (koreanName == null || koreanName.isBlank()) {
      return Optional.empty();
    }
    String normalized = Normalizer.normalize(koreanName.trim(), Normalizer.Form.NFC);
    return Optional.ofNullable(BY_KOREAN_NAME.get(normalized));
  }
}
