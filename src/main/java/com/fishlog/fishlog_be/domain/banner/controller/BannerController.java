package com.fishlog.fishlog_be.domain.banner.controller;

import com.fishlog.fishlog_be.domain.banner.service.BannerService;
import com.fishlog.fishlog_be.domain.fish.dto.SeasonalFishResponse;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banner")
public class BannerController implements BannerControllerSpec {

  /** 배너에 노출할 계절별 추천 어종 수. */
  private static final int SEASONAL_FISH_COUNT = 3;

  private final BannerService bannerService;

  @Override
  @GetMapping("/seasonal-fish")
  public BaseResponse<List<SeasonalFishResponse>> getSeasonalFish() {
    return BaseResponse.success(bannerService.getSeasonalFishRecommendations(SEASONAL_FISH_COUNT));
  }
}
