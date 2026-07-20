package com.fishlog.fishlog_be.domain.collection.controller;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.service.CollectionService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 사용자 도감(어종 인증) 조회 API. Swagger 문서는 {@link CollectionControllerSpec} 참고. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collections")
public class CollectionController implements CollectionControllerSpec {

  private final CollectionService collectionService;

  @Override
  @GetMapping
  public BaseResponse<CatchRecordResponse> getMyCatch(
      @RequestParam Long userId, @RequestParam Long fishId) {
    return BaseResponse.success(collectionService.getMyCatch(userId, fishId));
  }

  @Override
  @GetMapping("/dex")
  public BaseResponse<MyDexResponse> getMyDex(@RequestParam Long userId) {
    return BaseResponse.success(collectionService.getMyDex(userId));
  }
}
