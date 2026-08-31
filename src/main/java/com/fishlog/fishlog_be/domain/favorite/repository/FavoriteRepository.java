package com.fishlog.fishlog_be.domain.favorite.repository;

import com.fishlog.fishlog_be.domain.favorite.entity.Favorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

  /** 이미 찜했는지(추가 idempotent 판정). */
  boolean existsByUserIdAndSpotId(Long userId, Long spotId);

  /** 찜 해제(없어도 0건 삭제 → idempotent). */
  void deleteByUserIdAndSpotId(Long userId, Long spotId);

  /**
   * 사용자가 찜한 스팟 id 목록. 스팟 목록에 {@code isFavorite}를 O(1)로 얹기 위한 소스(집합 조회 후 메모리 병합). → {@code
   * SpotServiceImpl}
   */
  @Query("SELECT f.spotId FROM Favorite f WHERE f.userId = :userId")
  List<Long> findSpotIdsByUserId(@Param("userId") Long userId);

  /** 회원탈퇴 시 해당 사용자의 찜을 전부 삭제(plain Long이라 DB 캐스케이드 없음). */
  void deleteByUserId(Long userId);
}
