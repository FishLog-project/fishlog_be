package com.fishlog.fishlog_be.domain.collection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fishlog.fishlog_be.domain.collection.dto.DexEntryResponse;
import com.fishlog.fishlog_be.domain.collection.dto.MyDexResponse;
import com.fishlog.fishlog_be.domain.collection.repository.CatchRecordRepository;
import com.fishlog.fishlog_be.domain.fish.dto.FishListResponse;
import com.fishlog.fishlog_be.domain.fish.dto.FishSummaryResponse;
import com.fishlog.fishlog_be.domain.fish.entity.Rarity;
import com.fishlog.fishlog_be.domain.fish.service.FishService;
import com.fishlog.fishlog_be.global.ai.FishClassifyClient;
import com.fishlog.fishlog_be.global.image.FishImageService;
import com.fishlog.fishlog_be.global.s3.S3Service;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 내 도감 그리드의 이미지 선택 규칙. 핵심은 "잡은 칸은 어종 이미지, 못 잡은 칸은 그림자 이미지"를 <b>서버가</b> 고른다는 것이다(→ docs/media.md
 * §0).
 */
class CollectionServiceImplDexTest {

  private static final FishSummaryResponse SEA_BREAM =
      new FishSummaryResponse(
          1L,
          "감성돔",
          "http://localhost:8080/images/fish/black_seabream_image.png",
          Rarity.USUALLY,
          "바다");
  private static final FishSummaryResponse SEA_BASS =
      new FishSummaryResponse(
          3L, "농어", "http://localhost:8080/images/fish/seabass_image.png", Rarity.USUALLY, "바다");

  @Test
  @DisplayName("잡은 칸은 어종 이미지, 못 잡은 칸은 그림자 이미지 URL 을 내려준다")
  void picksShadowForUncaught() {
    CatchRecordRepository catchRecordRepository = mock(CatchRecordRepository.class);
    FishService fishService = mock(FishService.class);
    FishImageService fishImageService = mock(FishImageService.class);

    when(fishService.getFishList(any()))
        .thenReturn(FishListResponse.of(List.of(SEA_BREAM, SEA_BASS)));
    when(catchRecordRepository.findDistinctCaughtFishIds(7L)).thenReturn(List.of(1L)); // 감성돔만 잡음
    when(fishImageService.getShadowImageUrl("농어"))
        .thenReturn("http://localhost:8080/images/fish/seabass_shadow.png");

    CollectionServiceImpl service =
        new CollectionServiceImpl(
            catchRecordRepository,
            fishService,
            mock(FishClassifyClient.class),
            mock(S3Service.class),
            fishImageService,
            mock(CustomCatchService.class));

    MyDexResponse response = service.getMyDex(7L);

    DexEntryResponse caught = response.fishes().get(0);
    DexEntryResponse uncaught = response.fishes().get(1);

    assertThat(caught.caught()).isTrue();
    assertThat(caught.imageUrl())
        .isEqualTo("http://localhost:8080/images/fish/black_seabream_image.png");
    assertThat(uncaught.caught()).isFalse();
    // 못 잡은 칸에 어종 이미지가 새어 나가면 안 된다(스포일러).
    assertThat(uncaught.imageUrl())
        .isEqualTo("http://localhost:8080/images/fish/seabass_shadow.png");
    assertThat(response.caughtCount()).isEqualTo(1);
  }
}
