package com.fishlog.fishlog_be.domain.collection.controller;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.service.CollectionService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 사용자 도감(어종 인증) 조회 API. */
@Tag(name = "Collection", description = "사용자 도감(어종 인증) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collections")
public class CollectionController {

  private final CollectionService collectionService;

  @Operation(
      summary = "내 어종 인증 조회",
      description =
          "특정 어종에 대해 내가 인증한 사진 목록과 잡은 횟수를 반환한다. 안 잡았으면 catchCount 0 · 빈 목록. "
              + "userId는 인증(JWT) 도입 전 임시 파라미터로, 추후 로그인 사용자로 대체된다.")
  @GetMapping
  public BaseResponse<CatchRecordResponse> getMyCatch(
      @Parameter(description = "사용자 id(임시). 추후 로그인 토큰으로 대체", example = "1") @RequestParam
          Long userId,
      @Parameter(description = "전체 도감 어종 id", example = "1") @RequestParam Long fishId) {
    return BaseResponse.success(collectionService.getMyCatch(userId, fishId));
  }

  @Operation(
      summary = "내 도감 조회",
      description =
          "전체 수집 대상 어종을 순서대로 반환하며, 각 어종에 대해 내가 잡았는지(caught)를 표시한다. 잡은 어종은 이미지, 못 잡은 어종은 그림자 처리에 쓴다. "
              + "totalCount·caughtCount로 도감 완성도를 함께 내려준다. "
              + "userId는 인증(JWT) 도입 전 임시 파라미터로, 추후 로그인 사용자로 대체된다.")
  @GetMapping("/dex")
  public BaseResponse<MyDexResponse> getMyDex(
      @Parameter(description = "사용자 id(임시). 추후 로그인 토큰으로 대체", example = "1") @RequestParam
          Long userId) {
    return BaseResponse.success(collectionService.getMyDex(userId));
  }
}
