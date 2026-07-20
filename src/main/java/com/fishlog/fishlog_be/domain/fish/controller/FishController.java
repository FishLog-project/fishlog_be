package com.fishlog.fishlog_be.domain.fish.controller;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 전체 도감(마스터 어종 카탈로그) 공개 조회 API. Swagger 문서는 {@link FishControllerSpec} 참고. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fish")
public class FishController implements FishControllerSpec {

  private final FishService fishService;

  @Override
  @GetMapping
  public BaseResponse<FishListResponse> getFishList(@RequestParam(required = false) String name) {
    return BaseResponse.success(fishService.getFishList(name));
  }

  @Override
  @GetMapping("/{id}")
  public BaseResponse<FishDetailResponse> getFishDetail(@PathVariable Long id) {
    return BaseResponse.success(fishService.getFishDetail(id));
  }
}
