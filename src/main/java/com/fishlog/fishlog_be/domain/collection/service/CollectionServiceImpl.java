package com.fishlog.fishlog_be.domain.collection.service;

import com.fishlog.fishlog_be.domain.collection.dto.CatchRecordResponse;
import com.fishlog.fishlog_be.domain.collection.entity.CatchRecord;
import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionServiceImpl implements CollectionService {

  private final CatchRecordRepository catchRecordRepository;

  @Override
  public CatchRecordResponse getMyCatch(Long userId, Long fishId) {
    // (userId, fishId) 로 인증 기록을 모아 응답으로 변환한다. 안 잡았으면 빈 리스트 → 200 + catchCount 0.
    List<CatchRecord> records = catchRecordRepository.findByUserIdAndFish_Id(userId, fishId);
    return CatchRecordResponse.of(records);
  }
}
