package com.fishlog.fishlog_be.domain.favorite.service;

import com.fishlog.fishlog_be.domain.favorite.entity.Favorite;
import com.fishlog.fishlog_be.domain.favorite.repository.FavoriteRepository;
import com.fishlog.fishlog_be.domain.spot.exception.SpotErrorCode;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FavoriteService} 구현.
 *
 * <p>스팟 존재 검증을 위해 {@code SpotRepository}를 직접 참조한다. 이는 {@code SpotService}↔{@code FavoriteService}
 * 순환 의존을 피하기 위한 선택으로, {@code RankingServiceImpl}이 타 도메인 repository를 read 용도로 직접 쓰는 예외와 동일한 성격이다. →
 * docs/architecture.md
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteServiceImpl implements FavoriteService {

  private final FavoriteRepository favoriteRepository;
  private final SpotRepository spotRepository;

  @Override
  @Transactional
  public void addFavorite(Long userId, Long spotId) {
    if (!spotRepository.existsById(spotId)) {
      throw new CustomException(SpotErrorCode.SPOT_NOT_FOUND);
    }
    if (favoriteRepository.existsByUserIdAndSpotId(userId, spotId)) {
      return; // 이미 찜함 → no-op
    }
    favoriteRepository.save(Favorite.builder().userId(userId).spotId(spotId).build());
  }

  @Override
  @Transactional
  public void removeFavorite(Long userId, Long spotId) {
    favoriteRepository.deleteByUserIdAndSpotId(userId, spotId); // 없어도 no-op
  }

  @Override
  public Set<Long> getFavoriteSpotIds(Long userId) {
    return new HashSet<>(favoriteRepository.findSpotIdsByUserId(userId));
  }

  @Override
  @Transactional
  public void deleteMyFavorites(Long userId) {
    favoriteRepository.deleteByUserId(userId);
  }
}
