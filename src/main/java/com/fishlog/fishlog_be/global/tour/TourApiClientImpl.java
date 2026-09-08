package com.fishlog.fishlog_be.global.tour;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.tour.dto.TourApiItem;
import com.fishlog.fishlog_be.global.tour.dto.TourApiResult;
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
 * {@link TourApiClient} 구현. 한국관광공사 KorService2 {@code locationBasedList2}를 호출하고 data.go.kr 표준
 * 래핑({@code response.header}/{@code response.body})을 파싱한다. → docs/external.md
 *
 * <p>data.go.kr 응답 특성 방어: {@code items.item}은 단건이면 객체, 다건이면 배열이고 결과가 없으면 {@code items}가 빈 문자열이다.
 * 그래서 레코드 바인딩 대신 {@link JsonNode}로 관대하게 파싱한다({@code FishingIndexClientImpl}과 동일 방식).
 *
 * <p>재시도: 입력 문제(4xx)는 재시도하지 않고, 연결 실패·타임아웃만 1회 재시도한다. serviceKey는 이중 인코딩을 피하려 직접 URI를 구성한다.
 */
@Component
@Slf4j
public class TourApiClientImpl implements TourApiClient {

  /** KorService2 성공 코드("0000") 또는 구형("0000"/"00") 모두 허용. */
  private static final String SUCCESS_CODE = "0000";

  private final RestClient restClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String baseUrl;
  private final String serviceKey;
  private final String mobileApp;

  public TourApiClientImpl(
      RestClient tourApiRestClient,
      @Value("${external.tour.base-url}") String baseUrl,
      @Value("${external.tour.service-key}") String serviceKey,
      @Value("${external.tour.mobile-app:fishlog}") String mobileApp) {
    this.restClient = tourApiRestClient;
    this.baseUrl = baseUrl;
    this.serviceKey = serviceKey;
    this.mobileApp = mobileApp;
  }

  @Override
  public TourApiResult search(
      int contentTypeId, double lat, double lng, int radius, int page, int numOfRows) {
    URI uri = buildUri(contentTypeId, lat, lng, radius, page, numOfRows);
    JsonNode response = fetchWithRetry(uri);
    return parse(response, page, numOfRows);
  }

  private URI buildUri(
      int contentTypeId, double lat, double lng, int radius, int page, int numOfRows) {
    // mapX=경도, mapY=위도, arrange=E(거리순). 파라미터는 모두 ASCII 라 별도 인코딩이 필요 없다.
    String query =
        "serviceKey="
            + encodeServiceKey(serviceKey)
            + "&MobileOS=ETC&MobileApp="
            + mobileApp
            + "&_type=json&arrange=E" // 가까운 순으로 호출
            + "&mapX="
            + lng
            + "&mapY="
            + lat
            + "&radius="
            + radius
            + "&contentTypeId="
            + contentTypeId
            + "&numOfRows="
            + numOfRows
            + "&pageNo="
            + page;
    return URI.create(baseUrl + "?" + query);
  }

  /** 연결 실패·타임아웃만 1회 재시도. 4xx(입력 문제)는 즉시 실패. */
  private JsonNode fetchWithRetry(URI uri) {
    try {
      return fetch(uri);
    } catch (RestClientResponseException e) {
      // 서버가 상태코드를 돌려준 경우: 4xx 는 재시도 무의미, 5xx 만 1회 재시도.
      if (e.getStatusCode().is4xxClientError()) {
        log.warn("[tour] TourAPI 4xx: {} {}", e.getStatusCode(), e.getMessage());
        throw new CustomException(TourErrorCode.TOUR_API_ERROR);
      }
      return retryOnce(uri, e);
    } catch (RuntimeException e) {
      // 연결 실패·타임아웃 등
      return retryOnce(uri, e);
    }
  }

  private JsonNode retryOnce(URI uri, RuntimeException first) {
    log.warn("[tour] TourAPI 호출 실패, 1회 재시도: {}", first.toString());
    try {
      return fetch(uri);
    } catch (RuntimeException e) {
      log.error("[tour] TourAPI 재시도 실패: {}", e.toString());
      throw new CustomException(TourErrorCode.TOUR_API_UNAVAILABLE);
    }
  }

  /** 단건 호출 → JSON 파싱 → resultCode 확인 → {@code response} 노드 반환. */
  private JsonNode fetch(URI uri) {
    String raw = restClient.get().uri(uri).retrieve().body(String.class);
    JsonNode response;
    try {
      response = objectMapper.readTree(raw).path("response");
    } catch (Exception e) {
      // 쿼터 초과 등에서 XML/HTML 이 오면 JSON 파싱이 깨진다.
      log.warn("[tour] TourAPI 응답 파싱 실패(비 JSON 추정): {}", preview(raw));
      throw new CustomException(TourErrorCode.TOUR_API_ERROR);
    }
    String resultCode = response.path("header").path("resultCode").asText("");
    if (!isSuccess(resultCode)) {
      String msg = response.path("header").path("resultMsg").asText("");
      log.warn("[tour] TourAPI resultCode={} resultMsg={}", resultCode, msg);
      throw new CustomException(TourErrorCode.TOUR_API_ERROR);
    }
    return response;
  }

  private TourApiResult parse(JsonNode response, int requestedPage, int requestedRows) {
    JsonNode body = response.path("body");
    int totalCount = body.path("totalCount").asInt(0);
    int pageNo = body.path("pageNo").asInt(requestedPage);
    int numOfRows = body.path("numOfRows").asInt(requestedRows);
    List<TourApiItem> items = new ArrayList<>();
    for (JsonNode item : extractItems(body)) {
      items.add(toItem(item));
    }
    return new TourApiResult(totalCount, pageNo, numOfRows, items);
  }

  /** {@code body.items.item} 을 리스트로. 단건=객체, 다건=배열, 결과 없음(items="")·누락 모두 빈 리스트로 방어. */
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

  private TourApiItem toItem(JsonNode n) {
    return new TourApiItem(
        emptyToNull(n.path("title").asText("")),
        emptyToNull(n.path("firstimage").asText("")),
        emptyToNull(n.path("firstimage2").asText("")),
        emptyToNull(n.path("addr1").asText("")),
        emptyToNull(n.path("addr2").asText("")),
        toDouble(n.path("mapx").asText("")),
        toDouble(n.path("mapy").asText("")));
  }

  private boolean isSuccess(String resultCode) {
    return SUCCESS_CODE.equals(resultCode) || "00".equals(resultCode);
  }

  private static String emptyToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
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
