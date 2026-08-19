package com.fishlog.fishlog_be.domain.spot.dto;

import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import io.swagger.v3.oas.annotations.media.Schema;

/** 스팟 상세의 예보 1건(예보일자 × 오전/오후). 파고·수온·물때 등 예보성 정보. → docs/spec.md */
@Schema(description = "스팟 예보(파고·수온·기온·유속·풍속·물때·낚시지수)")
public record ForecastResponse(
    @Schema(description = "예보 일자(YYYYMMDD)", example = "20260814") String predcYmd,
    @Schema(description = "오전/오후 구분", example = "1") String predcNoonSeCd,
    @Schema(description = "낚시지수(라벨)", example = "보통") String totalIndex,
    @Schema(description = "낚시지수 점수(값 없으면 null)", example = "60") Integer lastScr,
    @Schema(description = "물때 점수(값 없으면 null)", example = "50") Integer tdlvHrScr,
    @Schema(description = "물때 내용", example = "중조기") String tdlvHrCn,
    @Schema(description = "최소 파고(m)", example = "0.5") Double minWvhgt,
    @Schema(description = "최대 파고(m)", example = "1.0") Double maxWvhgt,
    @Schema(description = "최소 수온(℃)", example = "18.0") Double minWtem,
    @Schema(description = "최대 수온(℃)", example = "21.0") Double maxWtem,
    @Schema(description = "최소 기온(℃)", example = "20.0") Double minArtmp,
    @Schema(description = "최대 기온(℃)", example = "26.0") Double maxArtmp,
    @Schema(description = "최소 유속", example = "0.1") Double minCrsp,
    @Schema(description = "최대 유속", example = "0.6") Double maxCrsp,
    @Schema(description = "최소 풍속", example = "2.0") Double minWspd,
    @Schema(description = "최대 풍속", example = "5.0") Double maxWspd) {

  public static ForecastResponse from(SpotForecast f) {
    return new ForecastResponse(
        f.predcYmd(),
        f.predcNoonSeCd(),
        f.totalIndex(),
        f.lastScr(),
        f.tdlvHrScr(),
        f.tdlvHrCn(),
        f.minWvhgt(),
        f.maxWvhgt(),
        f.minWtem(),
        f.maxWtem(),
        f.minArtmp(),
        f.maxArtmp(),
        f.minCrsp(),
        f.maxCrsp(),
        f.minWspd(),
        f.maxWspd());
  }
}
