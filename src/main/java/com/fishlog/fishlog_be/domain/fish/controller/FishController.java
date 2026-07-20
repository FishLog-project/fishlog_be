package com.fishlog.fishlog_be.domain.fish.controller;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 전체 도감(마스터 어종 카탈로그) 공개 조회 API. */
@Tag(name = "Fish", description = "전체 도감(어종 카탈로그) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fish")
public class FishController {

  private final FishService fishService;

  @Operation(
      summary = "전체 도감 목록 조회 / 이름 검색",
      description =
          "수집 대상 어종 목록과 총 수를 반환한다. name 파라미터가 있으면 이름 완전일치로 필터하며(0~1건), 없으면 전체 수집 대상 어종 목록을 반환한다.")
  @GetMapping
  public BaseResponse<FishListResponse> getFishList(
      @Parameter(description = "어종명 완전일치 검색(선택). 예: 감성돔") @RequestParam(required = false)
          String name) {
    return BaseResponse.success(fishService.getFishList(name));
  }

  @Operation(summary = "어종 상세 조회", description = "어종 ID로 상세 정보를 반환한다. 없으면 404.")
  @GetMapping("/{id}")
  public BaseResponse<FishDetailResponse> getFishDetail(@PathVariable Long id) {
    return BaseResponse.success(fishService.getFishDetail(id));
  }
}
