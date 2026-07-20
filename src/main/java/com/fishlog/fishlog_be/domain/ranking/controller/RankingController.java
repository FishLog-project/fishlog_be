package com.fishlog.fishlog_be.domain.ranking.controller;

import com.fishlog.fishlog_be.domain.ranking.dto.RankingResponse;
import com.fishlog.fishlog_be.domain.ranking.service.RankingService;
import com.fishlog.fishlog_be.global.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 랭킹 API. 완성도·크기 두 기준을 경로로 구분한다. Swagger 문서는 {@link RankingControllerSpec} 참고. →
 * docs/ranking.md
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rankings")
public class RankingController implements RankingControllerSpec {

  private final RankingService rankingService;

  @Override
  @GetMapping("/completion")
  public BaseResponse<RankingResponse> getCompletionRanking(
      @RequestParam(required = false) Long userId) {
    return BaseResponse.success(rankingService.getCompletionRanking(userId));
  }

  @Override
  @GetMapping("/size")
  public BaseResponse<RankingResponse> getSizeRanking(@RequestParam(required = false) Long userId) {
    return BaseResponse.success(rankingService.getSizeRanking(userId));
  }
}
