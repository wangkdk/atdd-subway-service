package nextstep.subway.member;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.acceptance.AuthAcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.member.dto.MemberRequest;
import nextstep.subway.member.dto.MemberResponse;

public class MemberAcceptanceTest extends AcceptanceTest {
	public static final String EMAIL = "email@email.com";
	public static final String PASSWORD = "password";
	public static final String NEW_EMAIL = "newemail@email.com";
	public static final String NEW_PASSWORD = "newpassword";
	public static final int AGE = 20;
	public static final int NEW_AGE = 21;
	public static final Integer CHILD_AGE = 12;
	public static final Integer STUDENT_AGE = 18;

	@DisplayName("회원 정보를 관리한다.")
	@Test
	void manageMember() {
		// when
		ExtractableResponse<Response> createResponse = 회원_생성을_요청(EMAIL, PASSWORD, AGE);
		// then
		회원_생성됨(createResponse);

		// when
		ExtractableResponse<Response> findResponse = 회원_정보_조회_요청(createResponse);
		// then
		회원_정보_조회됨(findResponse, EMAIL, AGE);

		// when
		ExtractableResponse<Response> updateResponse = 회원_정보_수정_요청(createResponse, NEW_EMAIL, NEW_PASSWORD, NEW_AGE);
		// then
		회원_정보_수정됨(updateResponse);

		// when
		ExtractableResponse<Response> deleteResponse = 회원_삭제_요청(createResponse);
		// then
		회원_삭제됨(deleteResponse);
	}

	@DisplayName("나의 정보를 관리한다.")
	@Test
	void manageMyInfo() {
		// when
		ExtractableResponse<Response> createResponse = 회원_생성을_요청(EMAIL, PASSWORD, AGE);
		// then
		회원_생성됨(createResponse);

		// given
		ExtractableResponse<Response> loginResponse = AuthAcceptanceTest.로그인을_시도한다(EMAIL, PASSWORD);
		String token = loginResponse.as(TokenResponse.class).getAccessToken();
		// when
		ExtractableResponse<Response> myInfoResponse = 내_정보_조회_요청(token);

		// then
		내_정보_조회됨(myInfoResponse, EMAIL, AGE);

		// when
		ExtractableResponse<Response> updateResponse = 내_정보_수정_요청(token, NEW_EMAIL, NEW_PASSWORD, NEW_AGE);

		// then
		내_정보_수정됨(updateResponse);
		ExtractableResponse<Response> loginResponseAfterUpdate =
			AuthAcceptanceTest.로그인을_시도한다(NEW_EMAIL, NEW_PASSWORD);
		token = loginResponseAfterUpdate.as(TokenResponse.class).getAccessToken();
		ExtractableResponse<Response> myInfoResponseAfterUpdate = 내_정보_조회_요청(token);
		내_정보_조회됨(myInfoResponseAfterUpdate, NEW_EMAIL, NEW_AGE);

		// when
		ExtractableResponse<Response> deleteResponse = 내_정보_삭제_요청(token);
		내_정보_삭제됨(deleteResponse);

	}

	public static ExtractableResponse<Response> 회원_생성을_요청(String email, String password, Integer age) {
		MemberRequest memberRequest = new MemberRequest(email, password, age);

		return RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.body(memberRequest)
			.when().post("/members")
			.then().log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 회원_정보_조회_요청(ExtractableResponse<Response> response) {
		String uri = response.header("Location");

		return RestAssured
			.given().log().all()
			.accept(MediaType.APPLICATION_JSON_VALUE)
			.when().get(uri)
			.then().log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 회원_정보_수정_요청(ExtractableResponse<Response> response, String email,
		String password, Integer age) {
		String uri = response.header("Location");
		MemberRequest memberRequest = new MemberRequest(email, password, age);

		return RestAssured
			.given().log().all()
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.body(memberRequest)
			.when().put(uri)
			.then().log().all()
			.extract();
	}

	public static ExtractableResponse<Response> 회원_삭제_요청(ExtractableResponse<Response> response) {
		String uri = response.header("Location");
		return RestAssured
			.given().log().all()
			.when().delete(uri)
			.then().log().all()
			.extract();
	}

	public static void 회원_생성됨(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
	}

	public static void 회원_정보_조회됨(ExtractableResponse<Response> response, String email, int age) {
		MemberResponse memberResponse = response.as(MemberResponse.class);
		assertThat(memberResponse.getId()).isNotNull();
		assertThat(memberResponse.getEmail()).isEqualTo(email);
		assertThat(memberResponse.getAge()).isEqualTo(age);
	}

	public static void 회원_정보_수정됨(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
	}

	public static void 회원_삭제됨(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

	public static ExtractableResponse<Response> 내_정보_조회_요청(final String token) {
		return RestAssured
			.given().log().all()
			.auth().oauth2(token)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when().get("/members/me")
			.then().log().all().extract();
	}

	private ExtractableResponse<Response> 내_정보_수정_요청(final String token, final String newEmail,
		final String newPassword, final int newAge) {
		MemberRequest memberRequest = new MemberRequest(newEmail, newPassword, newAge);
		return RestAssured
			.given().log().all()
			.auth().oauth2(token)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.body(memberRequest)
			.when().put("/members/me")
			.then().log().all()
			.extract();
	}

	private ExtractableResponse<Response> 내_정보_삭제_요청(final String token) {
		return RestAssured
			.given().log().all()
			.auth().oauth2(token)
			.contentType(MediaType.APPLICATION_JSON_VALUE)
			.when().delete("/members/me")
			.then().log().all().extract();
	}

	private void 내_정보_조회됨(final ExtractableResponse<Response> response, final String email, final int age) {
		MemberResponse memberResponse = response.as(MemberResponse.class);
		assertAll(
			() -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value()),
			() -> assertThat(memberResponse.getEmail()).isEqualTo(email),
			() -> assertThat(memberResponse.getAge()).isEqualTo(age)
		);
	}

	private void 내_정보_수정됨(final ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
	}

	private void 내_정보_삭제됨(final ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
	}

}
