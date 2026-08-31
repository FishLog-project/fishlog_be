package com.fishlog.fishlog_be.domain.favorite.controller;

import com.fishlog.fishlog_be.domain.favorite.service.FavoriteService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 스팟 찜(즐겨찾기) API. 문서는 {@link FavoriteControllerSpec}. → docs/spec.md */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spots/{spotId}/favorite")
public class FavoriteController implements FavoriteControllerSpec {

  private final FavoriteService favoriteService;

  @Override
  @PostMapping
  public BaseResponse<Void> addFavorite(
      @AuthenticationPrincipal Long userId, @PathVariable Long spotId) {
    favoriteService.addFavorite(userId, spotId);
    return BaseResponse.success("스팟을 찜했습니다.", null);
  }

  @Override
  @DeleteMapping
  public BaseResponse<Void> removeFavorite(
      @AuthenticationPrincipal Long userId, @PathVariable Long spotId) {
    favoriteService.removeFavorite(userId, spotId);
    return BaseResponse.success("찜을 해제했습니다.", null);
  }
}
