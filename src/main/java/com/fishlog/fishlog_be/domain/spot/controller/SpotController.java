package com.fishlog.fishlog_be.domain.spot.controller;

import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.domain.spot.service.SpotService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 낚시 스팟 API. 문서는 {@link SpotControllerSpec}. → docs/spec.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spots")
public class SpotController implements SpotControllerSpec {

  private final SpotService spotService;

  @Override
  @GetMapping
  public BaseResponse<List<SpotResponse>> getSpots() {
    return BaseResponse.success(spotService.getSpots());
  }
}
