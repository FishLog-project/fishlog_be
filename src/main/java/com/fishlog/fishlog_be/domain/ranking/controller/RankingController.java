package com.fishlog.fishlog_be.domain.ranking.controller;

import com.fishlog.fishlog_be.domain.ranking.dto.RankingResponse;
import com.fishlog.fishlog_be.domain.ranking.service.RankingService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 사용자 랭킹 API. 완성도·크기 두 기준을 경로로 구분한다. → docs/ranking.md */
@Tag(name = "Ranking", description = "사용자 랭킹(도감 완성도·최대 크기) API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rankings")
public class RankingController {

  private final RankingService rankingService;

  @Operation(
      summary = "도감 완성도 랭킹",
      description =
          "고유 수집대상 어종 수 ÷ 전체 도감 어종 수 기준 순위. 본인 순위(me) + Top3 + 전체 순위를 반환한다. "
              + "userId는 인증(JWT) 도입 전 임시 파라미터로, 없으면 me는 null.")
  @GetMapping("/completion")
  public BaseResponse<RankingResponse> getCompletionRanking(
      @Parameter(description = "사용자 id(임시). 본인 순위 계산용. 추후 로그인 토큰으로 대체", example = "1")
          @RequestParam(required = false)
          Long userId) {
    return BaseResponse.success(rankingService.getCompletionRanking(userId));
  }

  @Operation(
      summary = "최대 어종 크기 랭킹",
      description =
          "잡은 어종 중 최대 크기(cm) 기준 순위. 본인 순위(me) + Top3 + 전체 순위를 반환한다. "
              + "userId는 인증(JWT) 도입 전 임시 파라미터로, 없으면 me는 null.")
  @GetMapping("/size")
  public BaseResponse<RankingResponse> getSizeRanking(
      @Parameter(description = "사용자 id(임시). 본인 순위 계산용. 추후 로그인 토큰으로 대체", example = "1")
          @RequestParam(required = false)
          Long userId) {
    return BaseResponse.success(rankingService.getSizeRanking(userId));
  }
}
