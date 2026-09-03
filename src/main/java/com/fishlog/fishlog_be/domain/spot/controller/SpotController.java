package com.fishlog.fishlog_be.domain.spot.controller;

import com.fishlog.fishlog_be.domain.spot.dto.PopularSpotResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotDetailResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.domain.spot.service.SpotService;
import com.fishlog.fishlog_be.domain.spot.service.SpotViewService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 낚시 스팟 API. 문서는 {@link SpotControllerSpec}. → docs/spec.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spots")
public class SpotController implements SpotControllerSpec {

  private final SpotService spotService;
  private final SpotViewService spotViewService;

  @Override
  @GetMapping
  public BaseResponse<List<SpotResponse>> getSpots(@AuthenticationPrincipal Long userId) {
    return BaseResponse.success(spotService.getSpots(userId));
  }

  @Override
  @GetMapping("/popular")
  public BaseResponse<List<PopularSpotResponse>> getPopularSpots() {
    return BaseResponse.success(spotService.getPopularSpots());
  }

  @Override
  @GetMapping("/{spotId}")
  public BaseResponse<SpotDetailResponse> getSpotDetail(
      @PathVariable Long spotId, @AuthenticationPrincipal Long userId, HttpServletRequest request) {
    // 스팟 존재 검증(없으면 여기서 404) 후 응답. 조회수는 비동기로 집계(사용자/IP 1일 1회).
    SpotDetailResponse response = spotService.getSpotDetail(spotId);
    spotViewService.recordView(spotId, userId, request.getRemoteAddr());
    return BaseResponse.success(response);
  }
}
