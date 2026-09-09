package com.fishlog.fishlog_be.global.image;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * 도감 이미지 URL 생성기. 기동 시 이미지 디렉터리를 <b>한 번</b> 스캔해 {@code "1_image" → "1_image.png"} 맵을 만들고, 요청 때는 그
 * 맵만 조회한다.
 *
 * <p><b>왜 스캔하나:</b> 확장자를 코드에 고정하면(예 {@code .png}) 나중에 jpg·webp 로 바뀔 때 코드를 고쳐야 한다. 파일 49장짜리 고정 리소스라
 * 기동 시 한 번 읽어 두면 그 비용도 사실상 없다. 파일이 없는 항목은 {@code null}을 돌려주므로 이미지를 아직 안 채운 상태에서도 API 는 정상 동작한다.
 *
 * <p><b>파일명은 ASCII 슬러그다.</b> 한글 어종명은 {@link FishImageName}이 영문 슬러그로 바꿔 준다({@code 감성돔} → {@code
 * black_seabream}). 파일명을 ASCII 로 고정하면 URL 퍼센트 인코딩, macOS 의 NFD 파일명, 컨테이너 로케일(ASCII) 문제가 한꺼번에 사라진다.
 * 한글은 슬러그를 찾는 <b>조회 키</b>로만 쓰이고 파일시스템·URL 에는 닿지 않는다. → docs/media.md §0
 *
 * <p><b>절대 URL 조립:</b> {@code fishlog.image.base-url}이 있으면 그 값을, 없으면 <b>현재 요청의 컨텍스트 경로</b>를 앞에 붙인다.
 * 배포 compose 가 {@code SERVER_FORWARD_HEADERS_STRATEGY=framework}를 켜 두어 프록시 뒤에서도 외부에서 보이는 스킴·호스트가
 * 잡힌다. 요청 컨텍스트 밖(스케줄러 등)에서 호출되면 접두사 없이 경로만 반환한다.
 */
@Slf4j
@Service
public class FishImageServiceImpl implements FishImageService {

  /** 도감 이미지 파일명 접미사 — {@code {어종명}_image}. */
  private static final String IMAGE_SUFFIX = "_image";

  /** 그림자 이미지 파일명 접미사 — {@code {어종명}_shadow}. */
  private static final String SHADOW_SUFFIX = "_shadow";

  /** 도감 외 어종 기본 이미지 파일명(확장자 제외). */
  private static final String BASIC_IMAGE_KEY = "basic_image";

  /** 이미지로 인정하는 확장자. 목록에 없는 파일(README·.gitkeep 등)은 스캔에서 무시한다. */
  private static final Set<String> IMAGE_EXTENSIONS =
      Set.of("png", "jpg", "jpeg", "webp", "gif", "svg");

  private final Path directory;
  private final String baseUrl;

  /** 확장자를 뗀 이름(NFC 정규화) → 실제 파일명. 기동 시 한 번 채우고 이후 읽기만 한다(불변 취급). */
  private final Map<String, String> fileNames = new HashMap<>();

  public FishImageServiceImpl(
      @Value("${fishlog.image.dir:" + DEFAULT_DIR + "}") String directory,
      @Value("${fishlog.image.base-url:}") String baseUrl) {
    this.directory = Path.of(directory);
    // 뒤 슬래시를 제거해 URL_PREFIX(앞 슬래시로 시작)와 합칠 때 "//"가 생기지 않게 한다.
    this.baseUrl = StringUtils.hasText(baseUrl) ? baseUrl.replaceAll("/+$", "") : "";
  }

  /** 이미지 디렉터리를 스캔해 파일명 맵을 채운다. 디렉터리가 없어도 기동을 막지 않는다(모든 URL 이 null 이 될 뿐). */
  @PostConstruct
  void scanImageFiles() {
    if (!Files.isDirectory(directory)) {
      log.warn("도감 이미지 디렉터리가 없습니다: {} (모든 이미지 URL 이 null 로 응답됩니다)", directory.toAbsolutePath());
      return;
    }
    try (Stream<Path> files = Files.list(directory)) {
      List<Path> imageFiles = files.filter(Files::isRegularFile).toList();
      for (Path file : imageFiles) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
          continue; // 확장자 없는 파일(.gitkeep 포함)은 이미지가 아니다.
        }
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!IMAGE_EXTENSIONS.contains(extension)) {
          continue;
        }
        // 파일명(ASCII 슬러그)이 곧 키다. 대소문자만 통일해 Windows/Linux 차이를 흡수한다.
        String key = fileName.substring(0, dot).toLowerCase(Locale.ROOT);
        String previous = fileNames.putIfAbsent(key, fileName);
        if (previous != null) {
          // 같은 이름에 확장자만 다른 파일이 둘 있으면 어느 쪽을 쓸지 정할 근거가 없다 → 먼저 읽힌 쪽을 쓰고 알린다.
          log.warn("도감 이미지 이름 충돌: {} 와 {} — 앞의 파일을 사용합니다", previous, fileName);
        }
      }
    } catch (IOException e) {
      log.error("도감 이미지 디렉터리 스캔 실패: {} — {}", directory.toAbsolutePath(), e.getMessage());
      return;
    }
    // 어떤 어종의 파일이 빠졌는지 기동 시점에 알려 준다 — 응답의 imageUrl:null 만 보고는 원인을 알 수 없기 때문이다.
    String missing =
        Arrays.stream(FishImageName.values())
            .filter(
                fish ->
                    !fileNames.containsKey(fish.getSlug() + IMAGE_SUFFIX)
                        || !fileNames.containsKey(fish.getSlug() + SHADOW_SUFFIX))
            .map(FishImageName::getKoreanName)
            .collect(Collectors.joining(", "));
    log.info(
        "도감 이미지 {}장 로드: dir={}, basic_image={}, 파일 없는 어종=[{}]",
        fileNames.size(),
        directory.toAbsolutePath(),
        fileNames.containsKey(BASIC_IMAGE_KEY) ? "있음" : "없음",
        missing.isEmpty() ? "없음" : missing);
  }

  @Override
  public String getFishImageUrl(String fishName) {
    return toUrl(key(fishName, IMAGE_SUFFIX));
  }

  @Override
  public String getShadowImageUrl(String fishName) {
    return toUrl(key(fishName, SHADOW_SUFFIX));
  }

  /**
   * 한글 어종명 + 접미사로 조회 키(파일명 앞부분)를 만든다.
   *
   * <p>매핑({@link FishImageName})에 없는 어종이면 null 을 돌려주고 조회를 건너뛴다 — 도감에 어종을 먼저 넣고 이미지를 나중에 붙이는 순서가
   * 가능해야 한다.
   */
  private String key(String fishName, String suffix) {
    Optional<FishImageName> imageName = FishImageName.ofKoreanName(fishName);
    if (imageName.isEmpty()) {
      log.debug("이미지 파일명 매핑에 없는 어종입니다: {} (FishImageName 에 추가 필요)", fishName);
      return null;
    }
    return imageName.get().getSlug() + suffix;
  }

  @Override
  public String getBasicImageUrl() {
    return toUrl(BASIC_IMAGE_KEY);
  }

  /** 조회 키로 절대 URL 을 만든다. 해당 파일이 없으면 null(응답 imageUrl 이 null 이 된다). */
  private String toUrl(String key) {
    if (key == null) {
      return null;
    }
    String fileName = fileNames.get(key);
    if (fileName == null) {
      // 파일 하나가 빠진 것은 요청마다 반복되는 상태라 WARN 으로 올리지 않는다(로그 폭주 방지).
      log.debug("도감 이미지 파일 없음: {} (dir={})", key, directory);
      return null;
    }
    // 파일명은 ASCII 슬러그라 사실상 그대로지만, 예상 밖 문자가 섞여도 안전하도록 경로 세그먼트로 인코딩한다.
    return resolveBaseUrl()
        + URL_PREFIX
        + UriUtils.encodePathSegment(fileName, StandardCharsets.UTF_8);
  }

  /** 설정된 base-url, 없으면 현재 요청의 컨텍스트 URL. 요청 밖이면 빈 문자열(경로만 반환). */
  private String resolveBaseUrl() {
    if (!baseUrl.isEmpty()) {
      return baseUrl;
    }
    try {
      return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    } catch (IllegalStateException e) {
      // 요청 스레드 밖(스케줄러·초기화 등)에서 호출된 경우. 상대 경로라도 프론트가 붙여 쓸 수 있다.
      return "";
    }
  }
}
