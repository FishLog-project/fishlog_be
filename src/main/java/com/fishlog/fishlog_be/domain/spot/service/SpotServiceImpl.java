package com.fishlog.fishlog_be.domain.spot.service;

import com.fishlog.fishlog_be.domain.spot.dto.ForecastResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotDetailResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.domain.spot.entity.MajorFish;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.exception.SpotErrorCode;
import com.fishlog.fishlog_be.domain.spot.repository.MajorFishRepository;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.forecast.ForecastService;
import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 낚시 스팟 조회. 목록은 DB 불변 정보만, 상세는 DB 정보 + 실시간 예보(Redis 캐시) 병합. → docs/spec.md, docs/external.md §1 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotServiceImpl implements SpotService {

  private final SpotRepository spotRepository;
  private final MajorFishRepository majorFishRepository;
  // 도메인 경계: 예보 외부연동은 global 서비스 인터페이스로만 접근.
  private final ForecastService forecastService;

  /** 스팟이 소규모(98개)라 전체 반환으로 충분하다. 영역(bbox)·반경 검색은 규모가 커지면 도입. → docs/geo.md */
  @Override
  public List<SpotResponse> getSpots() {
    return spotRepository.findAll().stream().map(SpotResponse::from).toList();
  }

  @Override
  public SpotDetailResponse getSpotDetail(Long id) {
    Spot spot =
        spotRepository
            .findById(id)
            .orElseThrow(() -> new CustomException(SpotErrorCode.SPOT_NOT_FOUND));

    List<String> majorFishes =
        majorFishRepository.findBySpot(spot).stream()
            .map(MajorFish::getFish)
            .map(f -> f.getName())
            .toList();

    // 예보성 정보는 저장하지 않고 실시간 병합. 실패/미매칭(담수 등)이면 forecast=null.
    List<SpotForecast> forecasts = forecastService.getForecast(spot.getName());
    List<ForecastResponse> forecast =
        forecasts.isEmpty() ? null : forecasts.stream().map(ForecastResponse::from).toList();

    return new SpotDetailResponse(
        spot.getId(),
        spot.getName(),
        spot.getLat(),
        spot.getLot(),
        spot.isProhibit(),
        majorFishes,
        forecast);
  }
}
