package com.fishlog.fishlog_be.global.tour;

import com.fishlog.fishlog_be.global.exception.model.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 관광 정보(TourAPI) 연동 에러 코드. → docs/external.md */
@Getter
@AllArgsConstructor
public enum TourErrorCode implements BaseErrorCode {
  INVALID_TYPE("T001", "지원하지 않는 관광 카테고리입니다. (관광지/숙박/음식점)", HttpStatus.BAD_REQUEST),
  TOUR_API_ERROR("T002", "관광 정보 조회에 실패했습니다.", HttpStatus.BAD_GATEWAY),
  TOUR_API_UNAVAILABLE("T003", "관광 정보 서버에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
