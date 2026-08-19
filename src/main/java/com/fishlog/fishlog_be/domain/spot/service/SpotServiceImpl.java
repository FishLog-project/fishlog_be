package com.fishlog.fishlog_be.domain.spot.service;

import com.fishlog.fishlog_be.domain.spot.dto.ForecastResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotDetailResponse;
import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.domain.spot.entity.MajorFish;
import com.fishlog.fishlog_be.domain.spot.entity.Spot;
import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import com.fishlog.fishlog_be.domain.spot.exception.SpotErrorCode;
import com.fishlog.fishlog_be.domain.spot.repository.MajorFishRepository;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.forecast.ForecastService;
import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 낚시 스팟 조회. 목록은 DB 불변 정보만, 상세는 DB 정보 + 실시간 예보(Redis 캐시) 병합. → docs/spec.md, docs/external.md §1 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SpotServiceImpl implements SpotService {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  // API predcYmd 포맷이 하이픈 포함(yyyy-MM-dd)이라 ISO_LOCAL_DATE로 맞춘다.
  private static final DateTimeFormatter YMD = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

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

    // 해양 스팟만 실시간 예보를 병합한다(내륙은 예보 대상 아님 → forecast=null).
    // 예보는 오늘 날짜(KST) + 현재 시각의 오전/오후 1건만 노출한다.
    ForecastResponse forecast =
        spot.getCategory() == SpotCategory.해양 ? resolveTodayForecast(spot.getName()) : null;

    return new SpotDetailResponse(
        spot.getId(),
        spot.getName(),
        spot.getLat(),
        spot.getLot(),
        spot.isProhibit(),
        spot.getCategory(),
        majorFishes,
        forecast);
  }

  /** 해양 스팟의 오늘·현재 시간대(오전/오후) 예보 1건. 미매칭/외부 실패 시 null(로그로 원인 구분). */
  private ForecastResponse resolveTodayForecast(String spotName) {
    String today = LocalDate.now(KST).format(YMD);
    String noon = LocalTime.now(KST).getHour() < 12 ? "오전" : "오후";
    List<SpotForecast> all = forecastService.getForecast(spotName);
    ForecastResponse forecast =
        all.stream()
            .filter(f -> today.equals(f.predcYmd()) && noon.equals(f.predcNoonSeCd()))
            .findFirst()
            .map(ForecastResponse::from)
            .orElse(null);

    if (forecast == null) {
      // 해양 스팟인데 예보가 없으면 데이터/연동 이상 신호이므로 warn.
      if (all.isEmpty()) {
        log.warn("[spot-detail] 해양 스팟 예보 미수집 spot='{}' (외부 실패 또는 스팟명↔seafsPstnNm 불일치)", spotName);
      } else {
        log.warn(
            "[spot-detail] 해양 스팟 예보 필터 미매칭 spot='{}' today={} noon={} 사용가능={}",
            spotName,
            today,
            noon,
            all.stream().map(f -> f.predcYmd() + "/" + f.predcNoonSeCd()).distinct().toList());
      }
    }
    return forecast;
  }
}
