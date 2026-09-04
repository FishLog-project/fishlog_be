package com.fishlog.fishlog_be.domain.collection.controller;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.dto.ClassifyResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.dto.VerifyResponse;
import com.fishlog.fishlog_be.domain.collection.service.CollectionService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 사용자 도감(어종 인증) API. Swagger 문서는 {@link CollectionControllerSpec} 참고. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collections")
public class CollectionController implements CollectionControllerSpec {

  private final CollectionService collectionService;

  @Override
  @GetMapping
  public BaseResponse<CatchRecordResponse> getMyCatch(
      @AuthenticationPrincipal Long userId, @RequestParam Long fishId) {
    return BaseResponse.success(collectionService.getMyCatch(userId, fishId));
  }

  @Override
  @GetMapping("/dex")
  public BaseResponse<MyDexResponse> getMyDex(@AuthenticationPrincipal Long userId) {
    return BaseResponse.success(collectionService.getMyDex(userId));
  }

  // 분류는 저장을 하지 않으므로 사용자 신원이 필요 없다(보호 엔드포인트인 것은 모델 서버를 아무나 못 쓰게 하기 위함).
  @Override
  @PostMapping(value = "/classify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<ClassifyResponse> classify(@RequestPart("image") MultipartFile image) {
    return BaseResponse.success(collectionService.classify(image));
  }

  @Override
  @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public BaseResponse<VerifyResponse> verify(
      @AuthenticationPrincipal Long userId,
      @RequestParam Long fishId,
      @RequestParam Double size,
      @RequestParam(required = false) String location,
      @RequestPart("image") MultipartFile image) {
    return BaseResponse.success(
        "어종 인증이 완료되었습니다.", collectionService.verify(userId, fishId, size, location, image));
  }
}
