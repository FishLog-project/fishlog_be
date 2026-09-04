package com.fishlog.fishlog_be.domain.tour.controller;

import com.fishlog.fishlog_be.domain.tour.dto.NearbyTourResponse;
import com.fishlog.fishlog_be.domain.tour.service.TourService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tours")
public class TourController implements TourControllerSpec {

  private final TourService tourService;

  @Override
  @GetMapping("/nearby")
  public BaseResponse<NearbyTourResponse> getNearbyTours(
      @RequestParam String type,
      @RequestParam double lat,
      @RequestParam double lng,
      @RequestParam(defaultValue = "5000") int radius,
      @RequestParam(defaultValue = "1") int page) {
    return BaseResponse.success(tourService.getNearbyTours(type, lat, lng, radius, page));
  }
}
