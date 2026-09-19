package com.fishlog.fishlog_be.global.congestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fishlog.fishlog_be.global.congestion.dto.CongestionRate;
import com.fishlog.fishlog_be.global.exception.CustomException;
import com.fishlog.fishlog_be.global.tour.TourErrorCode;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 집중률 API 응답 파싱·오류 처리.
 *
 * <p>여기 쓰인 JSON은 전부 <b>실호출에서 받은 실제 응답 형태</b>다. data.go.kr 응답은 정상/오류/결과없음의 생김새가 제각각이라, 이 방어가 무너지면
 * 오류를 "혼잡도 없음"으로 조용히 삼키게 된다.
 */
class CongestionClientImplTest {

  private static final String BASE = "https://api.test/tatsCnctrRatedList";

  private RestClient.Builder builder;
  private MockRestServiceServer server;
  private CongestionClientImpl client;

  @BeforeEach
  void setUp() {
    builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new CongestionClientImpl(builder.build(), BASE, "test-key", "fishlog");
  }

  private void expectBody(String body) {
    server
        .expect(ExpectedCount.once(), requestTo(Matchers.startsWith(BASE)))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  @Test
  @DisplayName("정상 응답을 관광지명·기준일·집중률로 파싱한다")
  void parsesSuccess() {
    expectBody(
        """
        {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[
          {"baseYmd":"20260919","areaCd":"52","areaNm":"전북특별자치도","signguCd":"52800","signguNm":"부안군","tAtsNm":"개암사","cnctrRate":"26.31"},
          {"baseYmd":"20260920","areaCd":"52","areaNm":"전북특별자치도","signguCd":"52800","signguNm":"부안군","tAtsNm":"개암사","cnctrRate":"47.58"}
        ]},"numOfRows":9999,"pageNo":1,"totalCount":2}}}
        """);

    List<CongestionRate> result = client.fetchBySigngu("52", "52800");

    assertThat(result)
        .containsExactly(
            new CongestionRate("20260919", "개암사", 26.31),
            new CongestionRate("20260920", "개암사", 47.58));
    server.verify();
  }

  @Test
  @DisplayName("데이터 없는 지역은 items가 빈 문자열로 와도 빈 리스트다 — 전남이 매일 이 경로를 탄다")
  void emptyItemsIsEmptyList() {
    expectBody(
        """
        {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":"","numOfRows":0,"pageNo":1,"totalCount":0}}}
        """);

    assertThat(client.fetchBySigngu("46", "46130")).isEmpty();
    server.verify();
  }

  @Test
  @DisplayName("결과가 1건이면 item이 배열이 아니라 객체로 온다")
  void singleItemIsObject() {
    expectBody(
        """
        {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":
          {"baseYmd":"20260919","tAtsNm":"채석강 (전북 서해안 국가지질공원)","cnctrRate":"39.61"}
        },"numOfRows":9999,"pageNo":1,"totalCount":1}}}
        """);

    assertThat(client.fetchBySigngu("52", "52800"))
        .containsExactly(new CongestionRate("20260919", "채석강 (전북 서해안 국가지질공원)", 39.61));
  }

  @Test
  @DisplayName("파라미터 오류는 래핑 없이 최상위 resultCode로 온다 — 결과 0건으로 착각하면 안 된다")
  void flatErrorEnvelopeIsNotSilentlyEmpty() {
    expectBody(
        """
        {"responseTime":"2026-09-18T18:52:37.410","resultCode":"11","resultMsg":"NO_MANDATORY_REQUEST_PARAMETERS_ERROR1(signguCd)"}
        """);

    assertThatThrownBy(() -> client.fetchBySigngu("52", ""))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", TourErrorCode.TOUR_API_ERROR);
  }

  @Test
  @DisplayName("표준 래핑 안의 실패 resultCode도 오류로 처리한다")
  void wrappedErrorCode() {
    expectBody(
        """
        {"response":{"header":{"resultCode":"22","resultMsg":"LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR"},"body":{}}}
        """);

    assertThatThrownBy(() -> client.fetchBySigngu("52", "52800"))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", TourErrorCode.TOUR_API_ERROR);
  }

  @Test
  @DisplayName("쿼터 초과 등으로 JSON이 아닌 응답이 오면 오류로 처리한다")
  void nonJsonResponse() {
    server
        .expect(ExpectedCount.once(), requestTo(Matchers.startsWith(BASE)))
        .andRespond(
            withSuccess(
                "<OpenAPI_ServiceResponse><cmmMsgHeader/></OpenAPI_ServiceResponse>",
                MediaType.TEXT_XML));

    assertThatThrownBy(() -> client.fetchBySigngu("52", "52800"))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", TourErrorCode.TOUR_API_ERROR);
  }

  @Test
  @DisplayName("값이 깨진 행은 건너뛰고 나머지는 살린다")
  void skipsBrokenRows() {
    expectBody(
        """
        {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
          {"baseYmd":"20260919","tAtsNm":"개암사","cnctrRate":"26.31"},
          {"baseYmd":"20260919","tAtsNm":"","cnctrRate":"50.0"},
          {"baseYmd":"20260919","tAtsNm":"수성당","cnctrRate":""},
          {"baseYmd":"","tAtsNm":"궁항","cnctrRate":"30.0"}
        ]},"totalCount":4}}}
        """);

    assertThat(client.fetchBySigngu("52", "52800"))
        .containsExactly(new CongestionRate("20260919", "개암사", 26.31));
  }

  @Test
  @DisplayName("5xx는 1회 재시도하고, 그래도 실패하면 503으로 수렴한다")
  void retriesOnceOnServerError() {
    server
        .expect(ExpectedCount.twice(), requestTo(Matchers.startsWith(BASE)))
        .andRespond(withServerError());

    assertThatThrownBy(() -> client.fetchBySigngu("52", "52800"))
        .isInstanceOf(CustomException.class)
        .hasFieldOrPropertyWithValue("errorCode", TourErrorCode.TOUR_API_UNAVAILABLE);
    server.verify();
  }
}
