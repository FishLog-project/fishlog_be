package com.fishlog.fishlog_be.domain.fish.service;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.dto.SeasonalFishResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Season;
import com.fishlog.fishlog_be.domain.fish.exception.FishErrorCode;
import com.fishlog.fishlog_be.domain.fish.repository.FishRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.image.FishImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FishServiceImpl implements FishService {

  private final FishRepository fishRepository;
  // 도감 이미지 URL 은 DB 컬럼이 아니라 어종명으로 만드는 값이다 — 엔티티→DTO 변환이 일어나는 이곳에서 한 번에 붙인다.
  private final FishImageService fishImageService;

  @Override
  public FishListResponse getFishList(String name) {
    // name 이 없으면(null·공백) 전체 목록, 있으면 이름 완전일치로 필터한다.
    // 두 경로 모두 최종적으로 같은 변환(toSummary)을 거쳐 FishListResponse 로 감싸 응답 형태를 통일한다.
    List<Fish> fishes;
    if (name == null || name.isBlank()) {
      fishes = fishRepository.findAllByOrderByIdAsc();
    } else {
      // 이름 완전일치: 있으면 1건 리스트, 없으면 빈 리스트(예외 아님 → 200 + totalCount:0).
      fishes = fishRepository.findByName(name).map(List::of).orElseGet(List::of);
    }
    return FishListResponse.of(fishes.stream().map(this::toSummary).toList());
  }

  @Override
  public FishDetailResponse getFishDetail(Long id) {
    Fish fish = getFishEntity(id);
    return FishDetailResponse.of(fish, fishImageService.getFishImageUrl(fish.getName()));
  }

  @Override
  public Fish getFishEntity(Long id) {
    return fishRepository
        .findById(id)
        .orElseThrow(() -> new CustomException(FishErrorCode.FISH_NOT_FOUND));
  }

  @Override
  public List<SeasonalFishResponse> getFishInSeason(Season season) {
    // 어종 수가 적어(도감 규모) 전체를 읽어 계절 플래그로 메모리 필터한다.
    return fishRepository.findAllByOrderByIdAsc().stream()
        .filter(season::matches)
        .map(
            fish -> SeasonalFishResponse.of(fish, fishImageService.getFishImageUrl(fish.getName())))
        .toList();
  }

  /** 어종 엔티티에 도감 이미지 URL 을 붙여 목록 항목으로 만든다. 이미지 파일이 없으면 URL 은 null 이다. */
  private FishSummaryResponse toSummary(Fish fish) {
    return FishSummaryResponse.of(fish, fishImageService.getFishImageUrl(fish.getName()));
  }
}
