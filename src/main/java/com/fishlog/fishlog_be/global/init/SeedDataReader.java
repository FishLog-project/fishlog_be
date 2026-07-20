package com.fishlog.fishlog_be.global.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.init.dto.FishContentSeedData;
import com.fishlog.fishlog_be.global.init.dto.SpotFishSeedData;
import com.fishlog.fishlog_be.global.init.dto.SpotSeedData;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 낚시 스팟 시드 JSON 파일을 읽어 DTO 로 역직렬화한다.
 *
 * <p>수집기(data/spot/seed.py)가 생성한 {@code spots_seed.json}, {@code spot_fish_seed.json}을 읽는다. 위치는
 * {@code fishlog.seed.*-location} 프로퍼티로 재정의할 수 있으며(기본값은 프로젝트 상대 경로 file:), Spring {@link
 * ResourceLoader} 규칙에 따라 {@code classpath:}·{@code file:} 접두사를 지원한다.
 *
 * <p>실제 DB 적재는 엔티티/레포지토리(#12) 이후 {@link SeedDataInitializer}에서 수행한다. → docs/external.md §1
 */
@Component
public class SeedDataReader {

  // 정적 시드 JSON 파싱 전용. 웹 계층 설정과 무관하므로 전용 인스턴스를 둔다
  // (ObjectMapper 자동설정 빈에 의존하지 않음).
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ResourceLoader resourceLoader;

  @Value("${fishlog.seed.spots-location:file:data/spot/spots_seed.json}")
  private String spotsLocation;

  @Value("${fishlog.seed.spot-fish-location:file:data/spot/spot_fish_seed.json}")
  private String spotFishLocation;

  @Value("${fishlog.seed.fish-content-location:file:data/fish/fish_content_seed.json}")
  private String fishContentLocation;

  public SeedDataReader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  /** spots_seed.json → 스팟 불변 정보(name/lat/lot). */
  public SpotSeedData readSpots() {
    return read(spotsLocation, SpotSeedData.class);
  }

  /** spot_fish_seed.json → (스팟, 어종) 페어. */
  public SpotFishSeedData readSpotFishes() {
    return read(spotFishLocation, SpotFishSeedData.class);
  }

  /** fish_content_seed.json → 어종 도감 콘텐츠(설명·서식지). */
  public FishContentSeedData readFishContents() {
    return read(fishContentLocation, FishContentSeedData.class);
  }

  private <T> T read(String location, Class<T> type) {
    Resource resource = resourceLoader.getResource(location);
    if (!resource.exists()) {
      throw new IllegalStateException(
          "시드 파일을 찾을 수 없습니다: " + location + " (data/spot/seed.py 로 먼저 생성하세요)");
    }
    try (InputStream in = resource.getInputStream()) {
      return objectMapper.readValue(in, type);
    } catch (IOException e) {
      throw new IllegalStateException("시드 파일 읽기 실패: " + location, e);
    }
  }
}
