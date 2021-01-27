package nextstep.subway.path;

import static io.restassured.RestAssured.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.acceptance.AuthAcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.line.acceptance.LineAcceptanceTest;
import nextstep.subway.line.acceptance.LineSectionAcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.PathResponse;
import nextstep.subway.member.MemberAcceptanceTest;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;

@DisplayName("지하철 경로 조회")
public class PathAcceptanceTest extends AcceptanceTest {

	private LineResponse 신분당선;
	private LineResponse 이호선;
	private LineResponse 삼호선;
	private LineResponse 사호선;
	private StationResponse 강남역;
	private StationResponse 양재역;
	private StationResponse 교대역;
	private StationResponse 남부터미널역;
	private StationResponse 용산역;
	private StationResponse 이수역;

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

		강남역 = StationAcceptanceTest.지하철역_등록되어_있음("강남역").as(StationResponse.class);
		양재역 = StationAcceptanceTest.지하철역_등록되어_있음("양재역").as(StationResponse.class);
		교대역 = StationAcceptanceTest.지하철역_등록되어_있음("교대역").as(StationResponse.class);
		남부터미널역 = StationAcceptanceTest.지하철역_등록되어_있음("남부터미널역").as(StationResponse.class);
		용산역 = StationAcceptanceTest.지하철역_등록되어_있음("용산역").as(StationResponse.class);
		이수역 = StationAcceptanceTest.지하철역_등록되어_있음("이수역").as(StationResponse.class);

		신분당선 = 지하철_노선_등록되어_있음("신분당선", "bg-red-600", 강남역, 양재역, 10);
		이호선 = 지하철_노선_등록되어_있음("이호선", "bg-green-600", 교대역, 강남역, 10);
		삼호선 = 지하철_노선_등록되어_있음("삼호선", "bg-orange-600", 교대역, 양재역, 5);
		사호선 = 지하철_노선_등록되어_있음("사호선", "bg-blue-600", 용산역, 이수역, 15);

		지하철_노선에_지하철역_등록되어_있음(삼호선, 교대역, 남부터미널역, 3);
	}

	@DisplayName("최단 경로 조회")
	@Test
	void findPath() {
		final int expectedFare = 1250;
		// When 지하철 경로 조회 요청
		ExtractableResponse<Response> response = 지하철_경로_조회_요청(양재역.getId(), 교대역.getId());
		// Then 지하철 경로 응답됨
		지하철_경로_응답됨(response, 5, expectedFare, 양재역, 남부터미널역, 교대역);
	}

	@DisplayName("최단 경로 조회 예외 - 출발역과 도착역이 같은 경우")
	@Test
	void findPathThrowExceptionWhenSameSourceAndTarget() {
		// When 지하철 경로 조회 요청
		ExtractableResponse<Response> response = 지하철_경로_조회_요청(양재역.getId(), 양재역.getId());

		// Then 지하철 경로 조회 예외 발생
		지하철_경로_조회_예외_발생(response);
	}

	@DisplayName("최단 경로 조회 예외 - 출발역과 도착역이 연결되어 있지 않은 경우")
	@Test
	void findPathThrowExceptionWhen() {
		// When 지하철 경로 조회 요청
		ExtractableResponse<Response> response = 지하철_경로_조회_요청(용산역.getId(), 남부터미널역.getId());

		// Then 지하철 경로 조회 예외 발생
		지하철_경로_조회_예외_발생(response);
	}

	@DisplayName("최단 경로 조회 예외 - 존재하지 않은 출발역이나 도착역을 조회할 경우")
	@Test
	void findPathThrowExceptionWhenNotExistsStation() {
		// When 지하철 경로 조회 요청
		ExtractableResponse<Response> response = 지하철_경로_조회_요청(999L, 998L);

		// Then 지하철 경로 조회 예외 발생
		지하철_경로_조회_예외_발생(response);
	}

	@DisplayName("로그인한 회원의 나이에 따라 경로 조회시 요금 정보에 반영되어 조회")
	@Test
	void findPathWithAgeFare() {
		final int expectedFare = 450;
		final String email = MemberAcceptanceTest.EMAIL;
		final String password = MemberAcceptanceTest.PASSWORD;
		final int age = MemberAcceptanceTest.CHILD_AGE;

		MemberAcceptanceTest.회원_생성을_요청(email, password, age);

		ExtractableResponse<Response> loginResponse = AuthAcceptanceTest.로그인을_시도한다(email, password);
		String token = loginResponse.as(TokenResponse.class).getAccessToken();

		// When 지하철 경로 조회 요청
		ExtractableResponse<Response> response = 회원으로_지하철_경로_조회_요청(token, 양재역.getId(), 교대역.getId());
		// Then 지하철 경로 응답됨
		지하철_경로_응답됨(response, 5, expectedFare, 양재역, 남부터미널역, 교대역);
	}

	private ExtractableResponse<Response> 회원으로_지하철_경로_조회_요청(final String token,
		final Long sourceStationId, final Long targetStationId) {
		return given().log().all()
			.accept(MediaType.ALL_VALUE)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.auth().oauth2(token)
			.when()
			.get(String.format("/paths?source=%d&target=%d", sourceStationId, targetStationId))
			.then().log().all()
			.extract();
	}

	private LineResponse 지하철_노선_등록되어_있음(final String name, final String color,
		final StationResponse upStation, final StationResponse downStation, final int distance) {
		return LineAcceptanceTest.지하철_노선_등록되어_있음(new LineRequest(name, color, upStation.getId(),
			downStation.getId(), distance)).as(LineResponse.class);
	}

	private void 지하철_노선에_지하철역_등록되어_있음(final LineResponse line, final StationResponse upStation,
		final StationResponse downStation, final int distance) {
		LineSectionAcceptanceTest.지하철_노선에_지하철역_등록_요청(line, upStation, downStation, distance);
	}

	private ExtractableResponse<Response> 지하철_경로_조회_요청(final Long sourceStationId, final Long targetStationId) {
		return given().log().all()
			.accept(MediaType.APPLICATION_JSON_VALUE)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when()
			.get(String.format("/paths?source=%d&target=%d", sourceStationId, targetStationId))
			.then().log().all()
			.extract();
	}

	private void 지하철_경로_응답됨(final ExtractableResponse<Response> response, final int distance,
		final int expectedFare, final StationResponse... stationResponses) {
		PathResponse pathResponse = response.as(PathResponse.class);
		assertAll(
			() -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
			() -> assertThat(pathResponse.getStations()).containsExactlyInAnyOrder(stationResponses),
			() -> assertThat(pathResponse.getDistance()).isEqualTo(distance),
			() -> assertThat(pathResponse.getFare()).isEqualTo(expectedFare)
		);
	}

	private void 지하철_경로_조회_예외_발생(final ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isNotEqualTo(HttpStatus.OK.value());
	}
}
