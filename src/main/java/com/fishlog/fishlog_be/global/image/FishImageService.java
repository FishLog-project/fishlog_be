package com.fishlog.fishlog_be.global.image;

/**
 * 도감 이미지(어종 이미지·그림자·도감 외 어종 기본 이미지) URL 제공자.
 *
 * <p><b>왜 DB가 아니라 파일 규칙인가:</b> 이 이미지들은 어종 수만큼만 존재하는 <b>불변 마스터 리소스</b>다(24종 × 2 + 기본 1 = 49장). 사용자
 * 업로드 사진(S3, 가변)과 달리 URL을 행마다 저장할 이유가 없고, 저장하면 시드/마이그레이션과 로컬·배포 도메인 차이를 매번 감당해야 한다. 그래서 {@code
 * fishes.image_url} 컬럼을 읽지 않고 <b>어종명으로 파일명을 규칙 생성</b>한다(한글 어종명 → 영문 슬러그는 {@link FishImageName}).
 *
 * <p><b>왜 id 가 아니라 어종명인가 ✅(확정):</b> {@code fishes.id}는 auto-increment 라 <b>DB를 초기화하면 값이 바뀐다</b>(실제로
 * 시드를 다시 넣으며 어긋났다). 어종명은 시드 파일에서 오므로 DB를 몇 번 날려도 그대로이고, {@code UNIQUE} 제약이 걸려 있으며, <b>AI 모델 서버와의 조인
 * 키</b>로도 이미 쓰이고 있다(→ docs/external.md §2). 이미지도 같은 키를 쓰는 것이 일관된다.
 *
 * <p>파일은 {@link #DEFAULT_DIR}(프로퍼티 {@code fishlog.image.dir}로 재정의)에 아래 이름으로 둔다. 확장자는 고정하지 않는다 — 기동
 * 시 폴더를 스캔해 실제 파일명을 찾는다.
 *
 * <ul>
 *   <li>{@code {어종명}_image.*} — 도감 이미지(잡은 어종) 예: {@code 감성돔_image.png}
 *   <li>{@code {어종명}_shadow.*} — 그림자 이미지(아직 못 잡은 어종) 예: {@code 감성돔_shadow.png}
 *   <li>{@code basic_image.*} — 도감 외 어종(사용자 등록 어종) 기본 이미지
 * </ul>
 *
 * <p>해당 파일이 없으면 예외가 아니라 {@code null}을 돌려준다. 이미지 큐레이션이 끝나지 않아도 API는 그대로 동작해야 하고, 응답의 {@code
 * imageUrl}은 원래 nullable 로 문서화돼 있다. → docs/media.md §0
 */
public interface FishImageService {

  /**
   * 이미지 파일을 두는 기본 디렉터리(프로젝트 루트 기준 상대 경로). 시드 JSON과 같은 {@code data/} 아래라 Dockerfile 의 COPY 에 이미
   * 포함된다.
   */
  String DEFAULT_DIR = "data/fish/images";

  /** 정적 서빙 URL 접두사. 리소스 핸들러({@code ImageResourceConfig})·시큐리티 공개 경로와 같은 값을 봐야 한다. */
  String URL_PREFIX = "/images/fish/";

  /**
   * 도감 어종의 이미지 URL({@code {어종명}_image}).
   *
   * @param fishName 도감 어종명({@code fishes.name}). 앞뒤 공백과 유니코드 정규화 차이는 내부에서 흡수한다
   * @return 절대 URL. 파일이 없거나 매핑({@link FishImageName})에 없는 어종이면 {@code null}
   */
  String getFishImageUrl(String fishName);

  /**
   * 도감 어종의 그림자(실루엣) 이미지 URL({@code {어종명}_shadow}). 아직 잡지 못한 칸에 쓴다.
   *
   * @return 절대 URL. 파일이 없거나 이름이 비면 {@code null}
   */
  String getShadowImageUrl(String fishName);

  /**
   * 도감 외 어종(사용자가 직접 등록한 어종)의 기본 이미지 URL({@code basic_image}).
   *
   * <p>사용자마다 어종이 달라 고정 이미지를 어종별로 둘 수 없으므로 전부 이 한 장을 공유한다. 사용자가 찍은 실제 사진은 상세 조회({@code GET
   * /api/collections/custom?customFishId=})에서 본다.
   *
   * @return 절대 URL. 파일이 없으면 {@code null}
   */
  String getBasicImageUrl();
}
