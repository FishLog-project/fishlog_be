package com.fishlog.fishlog_be.global.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fishlog.fishlog_be.global.forecast.dto.SpotForecast;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * {@link FishingIndexClient} 구현. data.go.kr 표준 응답({@code response.body.items.item[]})을 파싱한다. →
 * docs/external.md §1
 *
 * <p>serviceKey는 이중 인코딩을 피하기 위해 직접 URI를 구성한다(Decoding 키는 percent-encoding, Encoding 키는 그대로).
 */
@Component
@Slf4j
public class FishingIndexClientImpl implements FishingIndexClient {

  private static final List<String> GUBUNS = List.of("갯바위", "선상");
  private static final int NUM_OF_ROWS = 300; // 페이지당 최대
  private static final int MAX_PAGES = 50; // 폭주 방지 상한(전체 ~1,750건 / 300 → 6페이지 수준)

  private final RestClient restClient;
  private final ObjectMapper objectMapper =
      new ObjectMapper(); // 자동설정 빈에 의존하지 않음(SeedDataReader와 동일 패턴)

  private final String baseUrl;
  private final String serviceKey;

  public FishingIndexClientImpl(
      RestClient fishingIndexRestClient,
      @Value("${external.fishing-index.base-url}") String baseUrl,
      @Value("${external.fishing-index.service-key}") String serviceKey) {
    this.restClient = fishingIndexRestClient;
    this.baseUrl = baseUrl;
    this.serviceKey = serviceKey;
  }

  @Override
  public List<SpotForecast> fetchAll() {
    List<SpotForecast> result = new ArrayList<>();
    for (String gubun : GUBUNS) {
      collectGubun(gubun, result);
    }
    return result;
  }

  private void collectGubun(String gubun, List<SpotForecast> sink) {
    int collected = 0;
    Integer totalCount = null;
    for (int pageNo = 1; pageNo <= MAX_PAGES; pageNo++) {
      JsonNode body = fetchPageBody(gubun, pageNo);
      if (totalCount == null) {
        totalCount = body.path("totalCount").asInt(0);
      }
      List<JsonNode> items = extractItems(body);
      if (items.isEmpty()) {
        break;
      }
      for (JsonNode item : items) {
        sink.add(toForecast(item));
      }
      collected += items.size();
      if ((totalCount > 0 && collected >= totalCount) || items.size() < NUM_OF_ROWS) {
        break;
      }
    }
  }

  /** 단일 페이지 호출 → resultCode 확인 → body 노드 반환. */
  private JsonNode fetchPageBody(String gubun, int pageNo) {
    String query =
        "serviceKey="
            + encodeServiceKey(serviceKey)
            + "&type=json&gubun="
            + enc(gubun)
            + "&pageNo="
            + pageNo
            + "&numOfRows="
            + NUM_OF_ROWS;
    URI uri = URI.create(baseUrl + "?" + query);

    String raw = restClient.get().uri(uri).retrieve().body(String.class);
    JsonNode root;
    try {
      root = objectMapper.readTree(raw);
    } catch (Exception e) {
      // 인증 오류 등은 XML로 내려오기도 한다 → 원문 일부와 함께 실패 처리.
      throw new IllegalStateException(
          "바다낚시지수 응답 파싱 실패: "
              + (raw == null ? "null" : raw.substring(0, Math.min(300, raw.length()))),
          e);
    }
    JsonNode response = root.has("response") ? root.get("response") : root;
    JsonNode header = response.path("header");
    String code = header.path("resultCode").asText(null);
    if (code != null && !code.equals("00") && !code.equals("0")) {
      throw new IllegalStateException(
          "바다낚시지수 API 오류 resultCode=" + code + " msg=" + header.path("resultMsg").asText(""));
    }
    return response.path("body");
  }

  /** {@code items.item}이 배열/객체/누락인 세 경우를 모두 리스트로 정규화. */
  private List<JsonNode> extractItems(JsonNode body) {
    JsonNode item = body.path("items").path("item");
    List<JsonNode> list = new ArrayList<>();
    if (item.isArray()) {
      item.forEach(list::add);
    } else if (item.isObject()) {
      list.add(item); // 1건이면 배열이 아닌 객체
    }
    return list;
  }

  private SpotForecast toForecast(JsonNode n) {
    return new SpotForecast(
        text(n, "seafsPstnNm"),
        text(n, "predcYmd"),
        text(n, "predcNoonSeCd"),
        text(n, "totalIndex"),
        text(n, "lastScr"),
        text(n, "tdlvHrScr"),
        text(n, "tdlvHrCn"),
        number(n, "minWvhgt"),
        number(n, "maxWvhgt"),
        number(n, "minWtem"),
        number(n, "maxWtem"),
        number(n, "minArtmp"),
        number(n, "maxArtmp"),
        number(n, "minCrsp"),
        number(n, "maxCrsp"),
        number(n, "minWspd"),
        number(n, "maxWspd"));
  }

  private String text(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v == null || v.isNull()) {
      return null;
    }
    String s = v.asText().trim();
    return s.isEmpty() ? null : s;
  }

  /** 수치 필드를 관대하게 파싱한다(빈 값·비수치는 null). */
  private Double number(JsonNode n, String field) {
    String s = text(n, field);
    if (s == null) {
      return null;
    }
    try {
      return Double.valueOf(s);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** Decoding 키(+,/,= 포함)는 percent-encoding, 이미 인코딩된 Encoding 키(% 포함)는 그대로. */
  private String encodeServiceKey(String key) {
    return key.contains("%") ? key : enc(key);
  }

  private String enc(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
