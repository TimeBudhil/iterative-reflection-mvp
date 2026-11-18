package iterative_reflection.mvp.identity;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class IdentityHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@Test
	void signupCreatesStudentAndIgnoresRoleInPayload() {
		String email = uniqueEmail();
		signup(email, PASSWORD, adult(), "v1", "\"role\": \"ADMIN\"").expectStatus().isCreated();
		assertThat(jdbc.queryForObject("select role from users where email = ?", String.class, email))
				.isEqualTo("STUDENT");
	}

	@Test
	void signupRejectsUnder13AndCreatesNoUser() {
		String email = uniqueEmail();
		LocalDate almost13 = LocalDate.now(ZoneOffset.UTC).minusYears(13).plusDays(1);
		signup(email, PASSWORD, almost13, "v1", null)
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(userCount(email)).isZero();
	}

	@Test
	void signupAcceptsUserWhoTurns13Today() {
		signup(uniqueEmail(), PASSWORD, LocalDate.now(ZoneOffset.UTC).minusYears(13), "v1", null)
				.expectStatus().isCreated();
	}

	@Test
	void onlyAgeVerifiedAtIsStoredNeverABirthdate() {
		String email = uniqueEmail();
		signup(email, PASSWORD, adult(), "v1", null).expectStatus().isCreated();
		assertThat(jdbc.queryForObject("select age_verified_at from users where email = ?", Object.class, email))
				.isNotNull();
		List<String> columns = jdbc.queryForList(
				"select column_name from information_schema.columns where table_name = 'users'", String.class);
		assertThat(columns).isNotEmpty().noneMatch(c -> c.toLowerCase().contains("birth"));
	}

	@Test
	void signupWithoutTermsVersionIs400() {
		String email = uniqueEmail();
		signup(email, PASSWORD, adult(), null, null)
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors[?(@.field == 'acceptedTermsVersion')]").exists();
		assertThat(userCount(email)).isZero();
	}

	@Test
	void termsVersionAndUtcTimestampAreStored() {
		String email = uniqueEmail();
		signup(email, PASSWORD, adult(), "2026-01", null).expectStatus().isCreated();
		assertThat(jdbc.queryForObject("select accepted_terms_version from users where email = ?", String.class,
				email)).isEqualTo("2026-01");
		assertThat(jdbc.queryForObject("select accepted_terms_at from users where email = ?", Object.class, email))
				.isNotNull();
	}

	@Test
	void elevenCharacterPasswordIs400AndStoredPasswordIsBcrypt() {
		String shortEmail = uniqueEmail();
		signup(shortEmail, "12345678901", adult(), "v1", null)
				.expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors[?(@.field == 'password')]").exists();
		assertThat(userCount(shortEmail)).isZero();

		String email = uniqueEmail();
		signup(email, "123456789012", adult(), "v1", null).expectStatus().isCreated();
		String hash = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
		assertThat(hash).startsWith("$2").isNotEqualTo("123456789012");
	}

	@Test
	void duplicateEmailInAnyCaseIs409() {
		String email = uniqueEmail();
		signup(email, PASSWORD, adult(), "v1", null).expectStatus().isCreated();
		signup(email.toUpperCase(), PASSWORD, adult(), "v1", null)
				.expectStatus().isEqualTo(409)
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void signinSetsHttpOnlySameSiteLaxSessionCookie() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		String cookie = signin(email, PASSWORD, null).expectStatus().isOk().returnResult().getResponseHeaders()
				.getOrEmpty(HttpHeaders.SET_COOKIE).stream().filter(c -> c.startsWith("SESSION=")).findFirst()
				.orElseThrow();
		assertThat(cookie).contains("HttpOnly").contains("SameSite=Lax").doesNotContain("Secure");
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameGeneric401() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		String wrong = signin(email, "wrong password!!", null).expectStatus().isUnauthorized()
				.expectBody(String.class).returnResult().getResponseBody();
		String unknown = signin(uniqueEmail(), "wrong password!!", null).expectStatus().isUnauthorized()
				.expectBody(String.class).returnResult().getResponseBody();
		assertThat(wrong).isEqualTo(unknown).doesNotContain(email);
	}

	@Test
	void signinRotatesSessionIdAndInvalidatesTheOldOne() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		Session first = signIn(email, PASSWORD);
		Session second = signIn(email, PASSWORD, first);
		assertThat(second.sessionId()).isNotEqualTo(first.sessionId());
		http.get().uri("/api/me").headers(first::apply).exchange().expectStatus().isUnauthorized();
		http.get().uri("/api/me").headers(second::apply).exchange().expectStatus().isOk();
	}

	@Test
	void sessionIsStoredInTheDatabase() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		signIn(email, PASSWORD);
		assertThat(jdbc.queryForObject("select count(*) from spring_session where principal_name = ?",
				Integer.class, email)).isEqualTo(1);
	}

	@Test
	void signoutInvalidatesTheSession() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		Session session = signIn(email, PASSWORD);
		http.post().uri("/api/auth/signout").headers(session::apply).exchange().expectStatus().isNoContent();
		http.get().uri("/api/me").headers(session::apply).exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void meReturnsTheCurrentUser() {
		String email = uniqueEmail();
		signup(email, PASSWORD, adult(), "v1", null).expectStatus().isCreated();
		Session session = signIn(email, PASSWORD);
		http.get().uri("/api/me").headers(session::apply).exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.id").isNumber()
				.jsonPath("$.email").isEqualTo(email)
				.jsonPath("$.displayName").isEqualTo("Sam")
				.jsonPath("$.role").isEqualTo("STUDENT")
				.jsonPath("$.intakePromptDue").isEqualTo(false)
				.jsonPath("$.passwordHash").doesNotExist();
	}

	@Test
	void meIs401WhenAnonymous() {
		http.get().uri("/api/me").exchange().expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void adminCanReadPrometheusButStudentCannot() {
		String admin = createUser(uniqueEmail(), PASSWORD, "ADMIN", false);
		String student = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		http.get().uri("/actuator/prometheus").headers(signIn(admin, PASSWORD)::apply).exchange()
				.expectStatus().isOk();
		http.get().uri("/actuator/prometheus").headers(signIn(student, PASSWORD)::apply).exchange()
				.expectStatus().isForbidden();
	}

	@Test
	void stateChangingRequestWithoutCsrfTokenIs403() {
		http.post().uri("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
				.body(signupBody(uniqueEmail(), PASSWORD, adult(), "v1", null))
				.exchange().expectStatus().isForbidden()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		Session session = signIn(email, PASSWORD);
		http.post().uri("/api/auth/signout")
				.header(HttpHeaders.COOKIE, "SESSION=" + session.sessionId())
				.exchange().expectStatus().isForbidden();
	}

	@Test
	void corsAllowsOnlyTheConfiguredOrigin() {
		http.options().uri("/api/auth/signin")
				.header(HttpHeaders.ORIGIN, "https://evil.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.exchange().expectStatus().isForbidden();
		http.options().uri("/api/auth/signin")
				.header(HttpHeaders.ORIGIN, "http://localhost:5173")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.exchange().expectStatus().isOk()
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
				.expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
	}

	private static LocalDate adult() {
		return LocalDate.now(ZoneOffset.UTC).minusYears(20);
	}

	private int userCount(String email) {
		return jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, email);
	}

	private String signupBody(String email, String password, LocalDate birthdate, String terms, String extra) {
		StringBuilder b = new StringBuilder("{\"email\": \"").append(email).append("\", \"password\": \"")
				.append(password).append("\", \"displayName\": \"Sam\", \"birthdate\": \"").append(birthdate)
				.append("\"");
		if (terms != null) {
			b.append(", \"acceptedTermsVersion\": \"").append(terms).append("\"");
		}
		if (extra != null) {
			b.append(", ").append(extra);
		}
		return b.append("}").toString();
	}

	private ResponseSpec signup(String email, String password, LocalDate birthdate, String terms, String extra) {
		Csrf csrf = csrf();
		return http.post().uri("/api/auth/signup").headers(csrf::apply).contentType(MediaType.APPLICATION_JSON)
				.body(signupBody(email, password, birthdate, terms, extra)).exchange();
	}

}
