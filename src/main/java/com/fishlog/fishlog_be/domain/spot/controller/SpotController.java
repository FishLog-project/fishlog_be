package com.fishlog.fishlog_be.domain.spot.controller;

import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.domain.spot.service.SpotService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 낚시 스팟 API. → docs/spec.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spots")
@Tag(name = "Spot", description = "낚시 스팟 API")
public class SpotController {

  private final SpotService spotService;

  @GetMapping
  @Operation(
      summary = "낚시 스팟 목록",
      description = "지도 마커용 스팟 목록(id·name·lat·lot)을 전체 반환한다. 프론트(카카오맵)가 이 좌표로 마커를 표시한다.")
  public BaseResponse<List<SpotResponse>> getSpots() {
    return BaseResponse.success(spotService.getSpots());
  }
}
