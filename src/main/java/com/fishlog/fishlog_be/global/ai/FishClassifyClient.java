package com.fishlog.fishlog_be.global.ai;

import com.fishlog.fishlog_be.global.ai.dto.PredictResponse;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** 어종 분류 모델 서버({@code POST /predict}) 클라이언트. → docs/external.md §2 */
public interface FishClassifyClient {

  /**
   * 사진 1장을 모델 서버에 보내 어종 후보(Top-3)를 받는다.
   *
   * <p><b>실패를 두 갈래로 나눠 다룬다.</b>
   *
   * <ul>
   *   <li><b>입력 문제(모델 4xx)</b> → {@link com.fishlog.fishlog_be.global.exception.CustomException}을
   *       던진다. 같은 사진을 다시 보내도 결과가 같으므로 재시도하지 않고, 사용자에게 무엇이 잘못됐는지 알려야 한다.
   *   <li><b>가용성 문제(5xx·타임아웃·네트워크)</b> → 1회 재시도 후 {@link Optional#empty()}. 호출부가 "직접 선택" 대안 경로로
   *       안내하도록 예외 대신 빈 값으로 돌려준다.
   * </ul>
   *
   * @param image 사용자가 올린 원본 이미지. <b>리사이즈·재인코딩하지 않고 바이트 그대로</b> 전달한다 — 모델이 학습과 동일한 전처리를 하도록 맞춰져 있어
   *     앞단에서 JPEG를 다시 구우면 정확도가 조용히 떨어진다.
   * @return 모델 응답. 모델 서버에 닿지 못했으면 {@code empty}
   */
  Optional<PredictResponse> predict(MultipartFile image);
}
