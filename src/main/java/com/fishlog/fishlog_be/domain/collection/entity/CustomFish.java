package com.fishlog.fishlog_be.domain.collection.entity;

import com.fishlog.fishlog_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 도감 외 어종 — <b>사용자별</b> 어종 카탈로그. 도감({@code fishes})이 전체 사용자에게 공통인 확정 24종이라면, 이쪽은 사용자가 직접 이름을 붙여 만든
 * 자기만의 어종 목록이다.
 *
 * <p><b>왜 이름 그룹을 테이블로 승격했나:</b> 처음에는 {@code custom_catch_record.fish_name} 문자열로 묶었지만, 그러면 어종을 가리키는
 * <b>안정적인 id 가 없어</b> 상세 조회(`?customFishId=`)를 만들 수 없다. 이름을 URL 파라미터로 쓰는 방법은 인코딩·공백·대소문자 문제를 그대로
 * 끌어오고, 나중에 이름 수정 기능이 생기면 링크가 깨진다. 도감이 {@code fishes}(어종) + {@code catch_record}(기록)로 나뉘듯 여기도
 * {@code custom_fish}(어종) + {@code custom_catch_record}(기록)로 나눈 것이다.
 *
 * <p><b>사용자별인 이유:</b> 이름은 검증되지 않은 자유 텍스트다. 전역으로 공유하면 한 사용자의 오타("쏘가리"→"쏘가르")가 다른 사용자 목록에 나타나고, 반대로
 * 누군가 이름을 고치면 남의 기록까지 따라 바뀐다. {@code UNIQUE(user_id, name)}로 <b>한 사용자 안에서만</b> 같은 이름을 하나로 모은다.
 *
 * <p>{@code userId}는 {@code catch_record}·{@code custom_catch_record}와 동일하게 아직 FK 가 아닌 plain Long
 * 이다. → docs/auth-followup.md §1
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(
    name = "custom_fish",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_custom_fish_user_name",
            columnNames = {"user_id", "name"}))
public class CustomFish extends BaseTimeEntity {

  /** 수기 입력 어종명의 최대 길이(자). 컬럼 정의와 입력 검증이 같은 값을 보도록 여기서 한 번만 정의한다. */
  public static final int MAX_NAME_LENGTH = 30;

  /** 수기 입력 서식지의 최대 길이(자). 도감의 값 집합(`바다`·`강`·`저수지`·`하천`)을 넉넉히 담되 문장 입력은 막는 길이다. */
  public static final int MAX_HABITAT_LENGTH = 20;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 이 어종을 만든 사용자 id. {@code users.id}를 가리키지만 FK 관계는 아직 아니다(plain Long). */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  /**
   * 사용자가 직접 입력한 어종명. 이 사용자 안에서 유일하다({@code UNIQUE(user_id, name)}).
   *
   * <p>등록 시 앞뒤 공백을 제거해 저장하므로 {@code "쏘가리 "}와 {@code "쏘가리"}는 같은 행으로 모인다. 반대로 {@code "우럭"}과 {@code
   * "조피볼락"}은 같은 물고기라도 다른 행이다 — 도감 밖 어종은 정답 목록이 없어 동의어를 판정할 주체가 없기 때문이다.
   */
  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String name;

  /**
   * 주요 서식지(선택).
   *
   * <p><b>기록이 아니라 어종의 속성</b>이라 여기 있다("이 물고기가 원래 어디 사나"). 기록마다 들고 있던 시절에는 같은 어종인데 기록마다 값이 달라질 수 있어
   * 조회 때 "가장 최근 non-null"을 고르는 규칙이 필요했는데, 어종 행으로 올라오면서 그 규칙 자체가 사라졌다.
   */
  @Column(length = MAX_HABITAT_LENGTH)
  private String habitat;

  /**
   * 서식지를 갱신한다. 등록 때마다 값이 들어오면 <b>마지막에 적은 값이 이긴다</b> — 처음엔 비워 뒀다가 나중에 채워 넣는 흐름이 자연스럽고, 그게 사용자의 최신
   * 의도이기 때문이다. 반대로 이번에 안 적었다면({@code null}) 기존 값을 지우지 않는다.
   */
  public void updateHabitatIfPresent(String habitat) {
    if (habitat != null) {
      this.habitat = habitat;
    }
  }
}
