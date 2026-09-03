package com.fishlog.fishlog_be.domain.fish.service;

import com.fishlog.fishlog_be.domain.fish.dto.FishDetailResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.domain.fish.dto.SeasonalFishResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Fish;
import com.fishlog.fishlog_be.domain.fish.entity.Season;
import java.util.List;

/** 전체 도감(마스터 어종 카탈로그) 조회 서비스. */
public interface FishService {

  /**
   * 전체 도감 목록(수집 대상 어종)과 총 수를 조회한다.
   *
   * @param name 어종명 완전일치 필터. {@code null}·공백이면 전체 목록, 값이 있으면 일치하는 어종만(0~1건) 반환한다. 못 찾아도 예외가 아니라 빈
   *     목록이다.
   */
  FishListResponse getFishList(String name);

  /**
   * 어종 상세를 조회한다.
   *
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 수집 대상 어종이 없으면 {@code
   *     FISH_NOT_FOUND}
   */
  FishDetailResponse getFishDetail(Long id);

  /**
   * 도감 어종 엔티티를 조회한다.
   *
   * <p>다른 도메인이 {@code fishes}를 <b>연관관계로 참조해야 할 때</b>만 쓰는 진입점이다(예: {@code catch_record.fishes_id}).
   * 엔티티가 필요해도 상대 도메인의 repository 를 직접 주입하지 않도록 service 인터페이스에 열어 둔다 — 조회 결과를 응답으로 내보낼 때는 여전히 {@link
   * #getFishDetail(Long)} 같은 DTO 반환 메서드를 쓴다. → docs/architecture.md
   *
   * @throws com.fishlog.fishlog_be.global.exception.CustomException 어종이 없으면 {@code FISH_NOT_FOUND}
   */
  Fish getFishEntity(Long id);

  /** 특정 계절에 제철인 어종 전체를 조회한다(배너 "계절별 추천 어종"의 후보군). 정렬·개수 제한·랜덤 선택은 호출부(배너 서비스)의 책임이다. 없으면 빈 목록. */
  List<SeasonalFishResponse> getFishInSeason(Season season);
}
