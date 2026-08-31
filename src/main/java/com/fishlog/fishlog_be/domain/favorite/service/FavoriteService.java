package com.fishlog.fishlog_be.domain.favorite.service;

import java.util.Set;

/** 스팟 찜(즐겨찾기) 서비스. → docs/spec.md */
public interface FavoriteService {

  /** 찜 추가. 이미 찜했으면 no-op(idempotent). 스팟이 없으면 SPOT_NOT_FOUND. */
  void addFavorite(Long userId, Long spotId);

  /** 찜 해제. 찜 상태가 아니어도 성공(idempotent). */
  void removeFavorite(Long userId, Long spotId);

  /** 사용자가 찜한 스팟 id 집합(스팟 목록의 isFavorite 병합용). */
  Set<Long> getFavoriteSpotIds(Long userId);

  /** 회원탈퇴 등으로 해당 사용자의 찜을 모두 삭제한다. */
  void deleteMyFavorites(Long userId);
}
