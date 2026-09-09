package com.fishlog.fishlog_be.global.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 도감 이미지 URL 생성 규칙. 핵심은 세 가지다 — "한글 어종명을 ASCII 슬러그 파일명으로 바꿔 찾는다", "확장자를 코드에 박지 않는다", "파일이 없으면 예외가
 * 아니라 null".
 *
 * <p>요청 컨텍스트가 없는 단위 테스트라, base-url 을 주지 않으면 접두사 없이 경로만 나오는 동작도 여기서 함께 확인한다.
 */
class FishImageServiceImplTest {

  private static final String BASE = "https://api.fishlog.com";

  private static FishImageServiceImpl serviceOf(Path dir, String baseUrl) {
    FishImageServiceImpl service = new FishImageServiceImpl(dir.toString(), baseUrl);
    service.scanImageFiles();
    return service;
  }

  private static void createImage(Path dir, String fileName) throws IOException {
    Files.writeString(dir.resolve(fileName), "not-a-real-image");
  }

  @Test
  @DisplayName("한글 어종명을 영문 슬러그 파일명으로 바꿔 절대 URL 을 만든다")
  void buildsUrls(@TempDir Path dir) throws IOException {
    createImage(dir, "black_seabream_image.png");
    createImage(dir, "black_seabream_shadow.png");
    createImage(dir, "basic_image.png");

    FishImageServiceImpl service = serviceOf(dir, BASE);

    // URL 이 전부 ASCII 라 퍼센트 인코딩이 필요 없다(Swagger·로그에서 그대로 읽힌다).
    assertThat(service.getFishImageUrl("감성돔"))
        .isEqualTo(BASE + "/images/fish/black_seabream_image.png");
    assertThat(service.getShadowImageUrl("감성돔"))
        .isEqualTo(BASE + "/images/fish/black_seabream_shadow.png");
    assertThat(service.getBasicImageUrl()).isEqualTo(BASE + "/images/fish/basic_image.png");
  }

  @Test
  @DisplayName("어종명의 유니코드 정규화(NFD)·앞뒤 공백이 달라도 같은 이미지를 찾는다")
  void normalizesFishName(@TempDir Path dir) throws IOException {
    createImage(dir, "seabass_image.png");

    FishImageServiceImpl service = serviceOf(dir, BASE);

    assertThat(service.getFishImageUrl("  농어  ")).isNotNull();
    // macOS 파일시스템·일부 입력기가 주는 자모 분리형(NFD)도 같은 어종으로 본다.
    assertThat(service.getFishImageUrl(Normalizer.normalize("농어", Normalizer.Form.NFD)))
        .isNotNull();
  }

  @Test
  @DisplayName("확장자는 고정이 아니다 — 폴더에 있는 파일을 그대로 쓴다")
  void extensionIsNotHardcoded(@TempDir Path dir) throws IOException {
    createImage(dir, "korean_rockfish_image.webp");
    createImage(dir, "korean_rockfish_shadow.jpg");

    FishImageServiceImpl service = serviceOf(dir, BASE);

    assertThat(service.getFishImageUrl("우럭")).endsWith("_image.webp");
    assertThat(service.getShadowImageUrl("우럭")).endsWith("_shadow.jpg");
  }

  @Test
  @DisplayName("파일이 없거나 매핑에 없는 어종이면 예외가 아니라 null")
  void missingFilesReturnNull(@TempDir Path dir) throws IOException {
    createImage(dir, "crucian_carp_image.png"); // 그림자·기본 이미지는 없는 상태
    Files.writeString(dir.resolve("README.md"), "이미지가 아닌 파일은 무시한다");

    FishImageServiceImpl service = serviceOf(dir, BASE);

    assertThat(service.getFishImageUrl("붕어")).isNotNull();
    assertThat(service.getShadowImageUrl("붕어")).isNull();
    assertThat(service.getBasicImageUrl()).isNull();
    assertThat(service.getFishImageUrl("한번도없던어종")).isNull(); // FishImageName 매핑 없음
    assertThat(service.getFishImageUrl(null)).isNull();
    assertThat(service.getFishImageUrl("  ")).isNull();
  }

  @Test
  @DisplayName("디렉터리 자체가 없어도 기동을 막지 않는다(모든 URL 이 null)")
  void missingDirectoryIsNotFatal(@TempDir Path dir) {
    FishImageServiceImpl service = serviceOf(dir.resolve("no-such-dir"), BASE);

    assertThat(service.getFishImageUrl("감성돔")).isNull();
    assertThat(service.getBasicImageUrl()).isNull();
  }

  @Test
  @DisplayName("base-url 을 주지 않고 요청 컨텍스트도 없으면 경로만 반환한다")
  void withoutBaseUrlReturnsPath(@TempDir Path dir) throws IOException {
    createImage(dir, "basic_image.png");

    FishImageServiceImpl service = serviceOf(dir, "");

    assertThat(service.getBasicImageUrl()).isEqualTo("/images/fish/basic_image.png");
  }

  @Test
  @DisplayName("base-url 뒤 슬래시는 제거해 // 가 생기지 않게 한다")
  void trimsTrailingSlashOfBaseUrl(@TempDir Path dir) throws IOException {
    createImage(dir, "basic_image.png");

    FishImageServiceImpl service = serviceOf(dir, BASE + "/");

    assertThat(service.getBasicImageUrl()).isEqualTo(BASE + "/images/fish/basic_image.png");
  }
}
