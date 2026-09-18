package com.fishlog.fishlog_be.global.congestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.tour.TourErrorCode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link CongestionClient} 구현. 한국관광공사 {@code TatsCnctrRateService/tatsCnctrRatedList}(data.go.kr
 * 15128555)를 호출한다. → docs/external.md §2
 *
 * <p>TourAPI와 <b>같은 서비스키·같은 RestClient·같은 재시도 정책</b>을 쓴다(같은 기관 API이고 별도 활용신청 없이 동작하는 것이 실호출로 확인됐다).
 * 에러 코드도 {@link TourErrorCode}를 재사용해 관광 연동 실패를 한 곳에서 본다.
 *
 * <p><b>응답 껍데기가 두 가지다.</b> 정상일 때는 data.go.kr 표준 래핑({@code response.header}/{@code response.body})이
 * 오지만, 파라미터 오류는 래핑 없이 최상위에 {@code {"resultCode":"11","resultMsg":"..."}}로 온다. 두 형태를 모두 확인해야 오류를 "결과
 * 0건"으로 착각하지 않는다.
 */
@Component
@Slf4j
public class CongestionClientImpl implements CongestionClient {

  private static final String SUCCESS_CODE = "0000";

  /**
   * 한 번에 받는 행 수. 가장 큰 시군구(제주시 244곳 × 30일 = 7,320행)도 1콜에 들어가도록 잡았다. 한도가 "호출 수" 기준이라 행을 크게 받는 편이
   * 유리하다.
   */
  private static final int NUM_OF_ROWS = 9999;

  /** 페이지 순회 상한. 한 시군구가 이보다 커지는 일은 없고, 응답이 이상할 때 무한 루프를 막는 안전장치다. */
  private static final int MAX_PAGES = 3;

  private final RestClient restClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String baseUrl;
  private final String serviceKey;
  private final String mobileApp;

  public CongestionClientImpl(
      RestClient tourApiRestClient,
      @Value(
              "${external.tour.congestion-base-url:https://apis.data.go.kr/B551011/TatsCnctrRateService/tatsCnctrRatedList}")
          String baseUrl,
      @Value("${external.tour.service-key}") String serviceKey,
      @Value("${external.tour.mobile-app:fishlog}") String mobileApp) {
    this.restClient = tourApiRestClient;
    this.baseUrl = baseUrl;
    this.serviceKey = serviceKey;
    this.mobileApp = mobileApp;
  }

  @Override
  public List<CongestionRate> fetchBySigngu(String areaCd, String signguCd) {
    List<CongestionRate> collected = new ArrayList<>();
    // 진행 기준은 "받은 행 수"다. 담은 행 수(collected)로 세면 값이 깨져 버린 행만큼 모자란 것으로 보여
    // 이미 다 받은 뒤에도 다음 페이지를 한 번 더 호출하게 된다 — 하루 1,000건짜리 한도에서 그냥 새는 호출이다.
    int received = 0;
    int totalCount = Integer.MAX_VALUE;
    for (int page = 1; page <= MAX_PAGES && received < totalCount; page++) {
      JsonNode body = fetchWithRetry(buildUri(areaCd, signguCd, page));
      totalCount = body.path("totalCount").asInt(0);
      List<JsonNode> items = extractItems(body);
      if (items.isEmpty()) {
        break; // 데이터 없는 시군구(전남 등)는 여기서 즉시 빈 리스트로 끝난다.
      }
      received += items.size();
      items.forEach(n -> addIfValid(collected, n));
    }
    return collected;
  }

  private URI buildUri(String areaCd, String signguCd, int page) {
    // areaCd/signguCd 는 숫자 문자열이라 인코딩이 필요 없다. baseYmd 는 요청 파라미터가 아니다(넣으면 400).
    String query =
        "serviceKey="
            + encodeServiceKey(serviceKey)
            + "&MobileOS=ETC&MobileApp="
            + mobileApp
            + "&_type=json&areaCd="
            + areaCd
            + "&signguCd="
            + signguCd
            + "&numOfRows="
            + NUM_OF_ROWS
            + "&pageNo="
            + page;
    return URI.create(baseUrl + "?" + query);
  }

  /** 연결 실패·타임아웃만 1회 재시도. 4xx(입력 문제)는 즉시 실패. */
  private JsonNode fetchWithRetry(URI uri) {
    try {
      return fetch(uri);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().is4xxClientError()) {
        log.warn("[congestion] 집중률 API 4xx: {} {}", e.getStatusCode(), e.getMessage());
        throw new CustomException(TourErrorCode.TOUR_API_ERROR);
      }
      return retryOnce(uri, e);
    } catch (CustomException e) {
      throw e; // resultCode 오류는 재시도해도 같다.
    } catch (RuntimeException e) {
      return retryOnce(uri, e);
    }
  }

  private JsonNode retryOnce(URI uri, RuntimeException first) {
    log.warn("[congestion] 집중률 API 호출 실패, 1회 재시도: {}", first.toString());
    try {
      return fetch(uri);
    } catch (RuntimeException e) {
      log.error("[congestion] 집중률 API 재시도 실패: {}", e.toString());
      throw new CustomException(TourErrorCode.TOUR_API_UNAVAILABLE);
    }
  }

  /** 단건 호출 → JSON 파싱 → resultCode 확인 → {@code response.body} 노드 반환. */
  private JsonNode fetch(URI uri) {
    String raw = restClient.get().uri(uri).retrieve().body(String.class);
    JsonNode root;
    try {
      root = objectMapper.readTree(raw);
    } catch (Exception e) {
      log.warn("[congestion] 집중률 응답 파싱 실패(비 JSON 추정): {}", preview(raw));
      throw new CustomException(TourErrorCode.TOUR_API_ERROR);
    }
    // 파라미터 오류 등은 래핑 없이 최상위에 resultCode 가 온다.
    if (root.has("resultCode") && !isSuccess(root.path("resultCode").asText(""))) {
      log.warn(
          "[congestion] 집중률 API 오류 resultCode={} resultMsg={}",
          root.path("resultCode").asText(""),
          root.path("resultMsg").asText(""));
      throw new CustomException(TourErrorCode.TOUR_API_ERROR);
    }
    JsonNode response = root.path("response");
    String resultCode = response.path("header").path("resultCode").asText("");
    if (!isSuccess(resultCode)) {
      log.warn(
          "[congestion] 집중률 API resultCode={} resultMsg={}",
          resultCode,
          response.path("header").path("resultMsg").asText(""));
      throw new CustomException(TourErrorCode.TOUR_API_ERROR);
    }
    return response.path("body");
  }

  /** {@code body.items.item} 을 리스트로. 단건=객체, 다건=배열, 결과 없음(items="")·누락 모두 빈 리스트. */
  private List<JsonNode> extractItems(JsonNode body) {
    JsonNode item = body.path("items").path("item");
    if (item.isArray()) {
      List<JsonNode> list = new ArrayList<>();
      item.forEach(list::add);
      return list;
    }
    if (item.isObject()) {
      return List.of(item);
    }
    return List.of();
  }

  /** 이름·집중률이 온전한 행만 담는다. 깨진 행 하나가 전체 조회를 실패시키지는 않는다. */
  private void addIfValid(List<CongestionRate> target, JsonNode n) {
    String name = n.path("tAtsNm").asText("");
    String baseYmd = n.path("baseYmd").asText("");
    Double rate = toDouble(n.path("cnctrRate").asText(""));
    if (name.isBlank() || baseYmd.isBlank() || rate == null) {
      return;
    }
    target.add(new CongestionRate(baseYmd, name, rate));
  }

  private boolean isSuccess(String resultCode) {
    return SUCCESS_CODE.equals(resultCode) || "00".equals(resultCode);
  }

  private static Double toDouble(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(s.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Decoding 키(+,/,= 포함)는 percent-encoding 이 필요하고, 이미 인코딩된(%포함) 키는 그대로 쓴다. */
  private static String encodeServiceKey(String key) {
    if (key.contains("%")) {
      return key;
    }
    return URLEncoder.encode(key, StandardCharsets.UTF_8);
  }

  private static String preview(String s) {
    if (s == null) {
      return "null";
    }
    return s.length() <= 200 ? s : s.substring(0, 200);
  }
}
