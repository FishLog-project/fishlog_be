package com.fishlog.fishlog_be.global.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 어종명 ↔ 파일명 슬러그 매핑. 도감 24종이 빠짐없이, 중복 없이 들어 있어야 이미지가 전부 뜬다. */
class FishImageNameTest {

  @Test
  @DisplayName("도감 24종이 모두 매핑돼 있고 슬러그가 서로 겹치지 않는다")
  void coversWholeDex() {
    assertThat(FishImageName.values()).hasSize(24);
    assertThat(Arrays.stream(FishImageName.values()).map(FishImageName::getSlug).distinct().count())
        .isEqualTo(24);
    assertThat(
            Arrays.stream(FishImageName.values())
                .map(FishImageName::getKoreanName)
                .distinct()
                .count())
        .isEqualTo(24);
  }

  @Test
  @DisplayName("슬러그는 파일명·URL 에 그대로 쓸 수 있는 ASCII 소문자다")
  void slugIsUrlSafe() {
    String invalid =
        Arrays.stream(FishImageName.values())
            .map(FishImageName::getSlug)
            .filter(slug -> !slug.matches("[a-z0-9_]+"))
            .collect(Collectors.joining(", "));
    assertThat(invalid).isEmpty();
  }

  @Test
  @DisplayName("한글 어종명으로 찾고, 없는 이름은 빈 값이다")
  void lookupByKoreanName() {
    assertThat(FishImageName.ofKoreanName("감성돔")).contains(FishImageName.BLACK_SEABREAM);
    assertThat(FishImageName.BLACK_SEABREAM.getSlug()).isEqualTo("black_seabream");
    assertThat(FishImageName.ofKoreanName("없는어종")).isEmpty();
    assertThat(FishImageName.ofKoreanName(null)).isEmpty();
  }
}
