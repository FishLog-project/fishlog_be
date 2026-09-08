package com.fishlog.fishlog_be.global.config;

import com.fishlog.fishlog_be.global.image.FishImageService;
import java.nio.file.Path;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 도감 이미지 정적 서빙 설정. {@code /images/fish/**} 요청을 이미지 디렉터리({@code fishlog.image.dir}, 기본 {@code
 * data/fish/images})의 파일로 직접 응답한다.
 *
 * <p><b>왜 컨트롤러가 아니라 리소스 핸들러인가:</b> 49장짜리 고정 파일이라 비즈니스 로직이 전혀 없다. 스프링의 {@code
 * ResourceHttpRequestHandler}가 Range·If-Modified-Since·ETag·Content-Type 판별과 경로 이탈(../) 차단을 이미 해 준다
 * — 컨트롤러로 바이트를 흘리면 그걸 전부 직접 짜야 한다.
 *
 * <p>이미지는 파일명이 바뀌지 않는 한 내용도 바뀌지 않으므로 30일 public 캐시를 건다(교체 시에는 파일명을 바꾸거나 브라우저 강력 새로고침이 필요).
 *
 * <p>{@code classpath:} 가 아니라 파일시스템을 쓰는 이유는 {@code src/main/resources}가 설정 서브모듈({@code
 * be_config})이라 이미지를 넣을 자리가 아니기 때문이다. {@code data/} 는 시드 JSON 과 함께 Dockerfile 의 {@code COPY data
 * ./data} 로 이미 배포된다. → docs/media.md
 */
@Slf4j
@Configuration
public class ImageResourceConfig implements WebMvcConfigurer {

  /** 이미지가 바뀌지 않는 고정 리소스라 길게 잡는다. */
  private static final Duration CACHE_DURATION = Duration.ofDays(30);

  private final String location;

  public ImageResourceConfig(
      @Value("${fishlog.image.dir:" + FishImageService.DEFAULT_DIR + "}") String directory) {
    // 상대 경로를 절대 경로 URI 로 바꿔 둔다 — OS 별 구분자(윈도우 \)를 URL 로 넘기지 않기 위함이다.
    // 디렉터리가 없을 때 toUri()가 뒤 슬래시를 붙이지 않으므로 직접 보정한다(없으면 상위 폴더가 열린다).
    String uri = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
    this.location = uri.endsWith("/") ? uri : uri + "/";
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    log.info("도감 이미지 정적 서빙: {}** → {}", FishImageService.URL_PREFIX, location);
    registry
        .addResourceHandler(FishImageService.URL_PREFIX + "**")
        .addResourceLocations(location)
        .setCacheControl(CacheControl.maxAge(CACHE_DURATION).cachePublic());
  }
}
