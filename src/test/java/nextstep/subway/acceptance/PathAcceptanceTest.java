package nextstep.subway.acceptance;

import static nextstep.subway.acceptance.AcceptanceTestSteps.given;
import static nextstep.subway.acceptance.LineSteps.지하철_노선에_지하철_구간_생성_요청;
import static nextstep.subway.acceptance.StationSteps.지하철역_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@DisplayName("지하철 경로 검색")
class PathAcceptanceTest extends AcceptanceTest {
    private Long 교대역;
    private Long 강남역;
    private Long 양재역;
    private Long 남부터미널역;
    private Long 이호선;
    private Long 신분당선;
    private Long 삼호선;

    /**
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역  --- *3호선* ---   양재
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        교대역 = 지하철역_생성_요청(관리자, "교대역").jsonPath().getLong("id");
        강남역 = 지하철역_생성_요청(관리자, "강남역").jsonPath().getLong("id");
        양재역 = 지하철역_생성_요청(관리자, "양재역").jsonPath().getLong("id");
        남부터미널역 = 지하철역_생성_요청(관리자, "남부터미널역").jsonPath().getLong("id");

        이호선 = 지하철_노선_생성_요청("2호선", "green", 교대역, 강남역, 0, 10, 3);
        신분당선 = 지하철_노선_생성_요청("신분당선", "red", 강남역, 양재역, 900, 10, 5);
        삼호선 = 지하철_노선_생성_요청("3호선", "orange", 교대역, 남부터미널역, 1000, 2, 2);

        지하철_노선에_지하철_구간_생성_요청(관리자, 삼호선, createSectionCreateParams(남부터미널역, 양재역, 3, 2));
    }

    /**
     * Scenario: 비 로그인의 두 역의 최단 거리 경로를 조회
     * Given 지하철역이 등록되어있음
     * And 지하철 노선이 등록되어있음
     * And 지하철 노선에 지하철역이 등록되어있음
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 최단 거리 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금도 함께 응답함 (로그인을 하지 않아서 따로 혜택 적용이 안됨)
     */
    @Test
    @DisplayName("비로그인 시, 두 역의 최단 거리 경로를 조회한다.")
    void findPathByDistance() {
        // when
        ExtractableResponse<Response> response = 비회원_최단_경로_조회(교대역, 양재역);

        // then
        assertPathStationFare(response, 1250, 교대역, 남부터미널역, 양재역);
    }

    /**
     * Scenario: 청소년 로그인일 시 두 역의 최단 거리 경로를 조회
     * Given 지하철역이 등록되어있음
     * And 지하철 노선이 등록되어있음
     * And 지하철 노선에 지하철역이 등록되어있음
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 최단 거리 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금 (청소년이라 할인혜택 30% 적용) 도 함께 응답함
     */
    @Test
    @DisplayName("청소년의 거리 경로 조회")
    void findPathByDistanceForTeenager() {
        // when
        ExtractableResponse<Response> response = 연령별_최단_거리_경로_조회(청소년, 교대역, 양재역);

        // then
        assertPathStationFare(response, 1070, 교대역, 남부터미널역, 양재역);
    }

    /**
     * Scenario: 어린이 로그인일 시 두 역의 최단 거리 경로를 조회
     * Given 지하철역이 등록되어있음
     * And 지하철 노선이 등록되어있음
     * And 지하철 노선에 지하철역이 등록되어있음
     * When 출발역에서 도착역까지의 최단 거리 경로 조회를 요청
     * Then 최단 거리 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금 (어린이 할인혜택 50% 적용) 도 함께 응답함
     */
    @Test
    @DisplayName("어린이의 거리 경로 조회")
    void findPathByDistanceForChild() {
        // when
        ExtractableResponse<Response> response = 연령별_최단_거리_경로_조회(어린이, 교대역, 양재역);

        // then
        assertPathStationFare(response, 800, 교대역, 남부터미널역, 양재역);
    }

    private void assertPathStationFare(final ExtractableResponse<Response> response, final int fare, Long... 역경로) {
        assertThat(response.jsonPath().getInt("fare")).isEqualTo(fare);
        assertThat(response.jsonPath().getList("stations.id", Long.class)).containsExactly(역경로);
    }

    private ExtractableResponse<Response> 비회원_최단_경로_조회(Long source, Long target) {
        return RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get("/paths?source={sourceId}&target={targetId}", source, target)
                .then().log().all().extract();
    }

    private ExtractableResponse<Response> 연령별_최단_거리_경로_조회(String token, Long source, Long target) {
        return given(token).log().all()
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .when().get("/paths?source={sourceId}&target={targetId}", source, target)
            .then().log().all().extract();
    }

    /**
     * Given 지하철역이 등록되어있음
     * And 지하철 노선이 등록되어있음
     * And 지하철 노선에 지하철역이 등록되어있음
     * When 출발역에서 도착역까지의 최소 시간 기준으로 경로 조회를 요청
     * Then 최소 시간 기준 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * When 출발역에서 도착역까지의 최소 시간 기준으로 경로 조회를 요청
     * Then 최소 시간 기준 경로를 응답
     * And 총 거리와 소요 시간을 함께 응답함
     * And 지하철 이용 요금도 함께 응답함
     */
    @Test
    @DisplayName("두 역의 최소 시간 경로를 조회한다.")
    void findPathByMinimumDuration() {
        //when
        final ExtractableResponse<Response> response = 두_역의_최소_시간_경로_조회(교대역, 양재역);
        final List<Long> 역_정보 = response.jsonPath().getList("stations.id", Long.class);
        final int totalDistance = response.jsonPath().getInt("distance");
        final int totalDuration = response.jsonPath().getInt("duration");
        final int fare = response.jsonPath().getInt("fare");

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(역_정보).containsExactly(교대역, 남부터미널역, 양재역);
        assertThat(totalDistance).isEqualTo(5);
        assertThat(totalDuration).isEqualTo(4);
        assertThat(fare).isEqualTo(1250);
    }

    private ExtractableResponse<Response> 두_역의_최소_시간_경로_조회(final long source, final long target) {
        return RestAssured.given().log().all()
            .when().get("/paths/time?source={sourceId}&target={targetId}", source, target)
            .then().log().all()
            .extract();
    }

    private Long 지하철_노선_생성_요청(String name, String color, Long upStation, Long downStation, int price, int distance, int duration) {
        Map<String, String> lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", name);
        lineCreateParams.put("color", color);
        lineCreateParams.put("upStationId", upStation + "");
        lineCreateParams.put("downStationId", downStation + "");
        lineCreateParams.put("price", price + "");
        lineCreateParams.put("distance", distance + "");
        lineCreateParams.put("duration", duration + "");

        return LineSteps.지하철_노선_생성_요청(관리자, lineCreateParams).jsonPath().getLong("id");
    }

    private Map<String, String> createSectionCreateParams(Long upStationId, Long downStationId, int distance, int duration) {
        Map<String, String> params = new HashMap<>();
        params.put("upStationId", upStationId + "");
        params.put("downStationId", downStationId + "");
        params.put("distance", distance + "");
        params.put("duration", duration + "");
        return params;
    }
}
