package com.fishlog.fishlog_be.domain.spot.service;

import com.fishlog.fishlog_be.domain.spot.dto.SpotResponse;
import com.fishlog.fishlog_be.domain.spot.repository.SpotRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 낚시 스팟 조회. DB 불변 정보만 다룬다(예보성은 상세 조회에서 실시간 병합, #15). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotServiceImpl implements SpotService {

  private final SpotRepository spotRepository;

  /** 스팟이 소규모(49개)라 전체 반환으로 충분하다. 영역(bbox)·반경 검색은 규모가 커지면 도입. → docs/geo.md */
  @Override
  public List<SpotResponse> getSpots() {
    return spotRepository.findAll().stream().map(SpotResponse::from).toList();
  }
}
