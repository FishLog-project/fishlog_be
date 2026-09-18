package com.fishlog.fishlog_be.domain.tour.dto;

import com.fishlog.fishlog_be.domain.tour.entity.CongestionLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 관광지 당일 혼잡도. 값을 붙일 수 없으면 이 객체 자체가 {@code null}로 내려간다(→ 화면은 혼잡도 칩을 숨긴다).
 *
 * @param rate 집중률 지수 0~100. <b>퍼센트가 아니다</b> — 2018년 이후 피크를 100으로 정규화한 상대 지수다
 * @param level 지수를 사용자 표현으로 바꾼 등급(여유/보통/혼잡)
 * @param baseDate 예측 기준일(조회 당일)
 */
@Schema(description = "관광지 당일 혼잡도(한국관광공사 집중률 예측)")
public record CongestionResponse(
    @Schema(description = "집중률 지수 0~100(퍼센트 아님)", example = "39.6") double rate,
    @Schema(
            description = "혼잡도 등급",
            example = "여유",
            allowableValues = {"여유", "보통", "혼잡"})
        String level,
    @Schema(description = "예측 기준일(조회 당일)", example = "2026-09-19") LocalDate baseDate) {

  public static CongestionResponse of(double rate, LocalDate baseDate) {
    return new CongestionResponse(rate, CongestionLevel.from(rate).label(), baseDate);
  }
}
