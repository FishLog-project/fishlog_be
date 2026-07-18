package com.fishlog.fishlog_be.domain.fish.service;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.exception.FishErrorCode;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FishServiceImpl implements FishService {

  private final FishRepository fishRepository;

  @Override
  public FishListResponse getFishList(String name) {
    // name 이 없으면(null·공백) 전체 목록, 있으면 이름 완전일치로 필터한다.
    // 두 경로 모두 최종적으로 List<Fish> 를 FishListResponse.of(...) 로 감싸 응답 형태를 통일한다.
    List<Fish> fishes;
    if (name == null || name.isBlank()) {
      fishes = fishRepository.findByIsCollectibleTrueOrderByIdAsc();
    } else {
      // 이름 완전일치: 있으면 1건 리스트, 없으면 빈 리스트(예외 아님 → 200 + totalCount:0).
      fishes =
          fishRepository.findByNameAndIsCollectibleTrue(name).map(List::of).orElseGet(List::of);
    }
    return FishListResponse.of(fishes);
  }

  @Override
  public FishDetailResponse getFishDetail(Long id) {
    Fish fish =
        fishRepository
            .findByIdAndIsCollectibleTrue(id)
            .orElseThrow(() -> new CustomException(FishErrorCode.FISH_NOT_FOUND));
    return FishDetailResponse.from(fish);
  }
}
