package com.fishlog.fishlog_be.domain.collection.repository;

import com.fishlog.fishlog_be.domain.collection.entity.CustomFish;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomFishRepository extends JpaRepository<CustomFish, Long> {

  /**
   * 이 사용자의 어종 목록에서 같은 이름을 찾는다. 등록 시 "기존 어종에 기록을 덧붙일지, 새 어종을 만들지"를 가르는 조회다.
   *
   * <p>{@code UNIQUE(user_id, name)} 덕분에 결과는 0~1건이다.
   */
  Optional<CustomFish> findByUserIdAndName(Long userId, String name);

  /**
   * 소유자까지 함께 검증하는 단건 조회.
   *
   * <p>{@code findById}로 찾고 나서 {@code userId}를 비교하면 "남의 어종을 조회했다"와 "없는 어종을 조회했다"가 코드상 다른 분기가 되어,
   * 한쪽을 빠뜨리면 <b>남의 기록이 새어 나간다</b>. 조건을 쿼리에 넣어 두 경우 모두 빈 결과 → 같은 404 로 수렴시킨다(존재 여부 자체도 알려주지 않는다).
   */
  Optional<CustomFish> findByIdAndUserId(Long id, Long userId);

  /**
   * 회원탈퇴 시 해당 사용자의 어종 목록을 모두 삭제한다(하드).
   *
   * <p>기록({@code custom_catch_record})을 먼저 지운 뒤 호출해야 한다 — FK 가 걸려 있어 순서가 뒤집히면 제약 위반이 난다.
   */
  void deleteByUserId(Long userId);
}
