package com.fishlog.fishlog_be.domain.banner.service;

import com.fishlog.fishlog_be.domain.banner.dto.RecommendedSpotsResponse;
import com.fishlog.fishlog_be.domain.fish.dto.SeasonalFishResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Season;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import com.fishlog.fishlog_be.domain.spot.service.SpotService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerServiceImpl implements BannerService {

  /** 계절 판정은 한국 시각 기준(서버 타임존과 무관하게 KST 월로 고정). */
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final FishService fishService;
  private final SpotService spotService;

  @Override
  public List<SeasonalFishResponse> getSeasonalFishRecommendations(int count) {
    if (count <= 0) {
      return List.of();
    }
    Season season = Season.of(LocalDate.now(KST).getMonthValue());
    // 계절 판정·후보 조회는 fish 도메인에 위임하고, 배너는 랜덤 셔플·개수 제한만 담당한다.
    List<SeasonalFishResponse> candidates = new ArrayList<>(fishService.getFishInSeason(season));
    Collections.shuffle(candidates);
    return candidates.stream().limit(count).toList();
  }

  @Override
  public RecommendedSpotsResponse getRecommendedSpots() {
    // 분류별 조회수 최다 스팟 조회는 spot 도메인에 위임한다(순위 로직은 spot 소유).
    return new RecommendedSpotsResponse(
        spotService.getTopSpotByCategory(SpotCategory.해양).orElse(null),
        spotService.getTopSpotByCategory(SpotCategory.내륙).orElse(null));
  }
}
