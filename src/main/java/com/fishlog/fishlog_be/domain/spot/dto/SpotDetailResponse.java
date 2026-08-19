package com.fishlog.fishlog_be.domain.spot.dto;

import com.fishlog.fishlog_be.domain.spot.entity.SpotCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 스팟 상세 응답. DB 불변 정보(기본정보 + 대상 어종)에 실시간 예보를 병합한다.
 *
 * <p>{@code forecast}는 **오늘 날짜의 현재 시각(오전/오후) 예보 1건**이며, 외부 호출 실패·미매칭(담수 스팟 등)·해당 시간대 예보 없음이면 {@code
 * null}이다(base 정보는 항상 응답). → docs/spec.md
 */
@Schema(description = "스팟 상세(기본정보 + 대상 어종 + 실시간 예보)")
public record SpotDetailResponse(
    @Schema(description = "스팟 ID", example = "1") Long spotId,
    @Schema(description = "위치명", example = "가거도") String name,
    @Schema(description = "위도", example = "34.07308") double lat,
    @Schema(description = "경도", example = "125.08805") double lot,
    @Schema(description = "낚시 금지 여부", example = "false") boolean prohibit,
    @Schema(description = "분류(해양/내륙)", example = "해양") SpotCategory category,
    @Schema(description = "주요 대상 어종명 목록", example = "[\"감성돔\", \"참돔\"]") List<String> majorFishes,
    @Schema(description = "오늘·현재 시간대(오전/오후) 예보 1건. 내륙 스팟·미제공 시 null") ForecastResponse forecast) {}
