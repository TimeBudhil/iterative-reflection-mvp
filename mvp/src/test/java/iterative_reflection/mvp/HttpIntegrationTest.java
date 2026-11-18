package iterative_reflection.mvp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Base class for HTTP-level integration tests: the primary test seam (see SPEC.md "Testing Decisions").
 *
 * <p>Boots the full application on a random port in the {@code dev} profile (H2 in PostgreSQL mode with
 * Flyway migrations applied) and drives it over real HTTP through {@link #http}. Assert only on what a client
 * can observe: status codes, headers, bodies and what later requests return.
 *
 * <p>Override the {@link java.time.Clock} or {@link java.util.random.RandomGenerator} beans in a subclass with a
 * nested {@code @TestConfiguration} that declares a {@code @Primary} bean, or with {@code @MockitoBean}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@AutoConfigureMetrics
@ActiveProfiles("dev")
public abstract class HttpIntegrationTest {

	private static final AtomicLong EMAIL_SEQUENCE = new AtomicLong();

	@Autowired
	protected RestTestClient http;

	/** Direct database access for arranging fixtures and asserting on stored data. */
	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/** A fresh lower-case email, unique across the whole test run (the database is shared between test classes). */
	protected String uniqueEmail() {
		return "user" + EMAIL_SEQUENCE.incrementAndGet() + "@test.example";
	}

	/**
	 * Inserts an active user straight into the database (so tests can have ADMIN/COUNSELOR users or a
	 * {@code mustChangePassword} flag without going through signup) and returns its email. Display name is
	 * "Test User". Sign in with {@link #signIn(String, String)}.
	 */
	protected String createUser(String email, String password, String role, boolean mustChangePassword) {
		jdbc.update("""
				insert into users (email, password_hash, display_name, role, active, must_change_password, demo,
				    created_at, version)
				values (?, ?, 'Test User', ?, true, ?, false, current_timestamp, 0)
				""", email, passwordEncoder.encode(password), role, mustChangePassword);
		return email;
	}

	/** Creates a fresh STUDENT and signs in; the shortest way to get an authenticated session. */
	protected Session signedInStudent() {
		return signIn(createUser(uniqueEmail(), "correct horse battery", "STUDENT", false), "correct horse battery");
	}

	/** Signs in over HTTP and returns the session (SESSION cookie plus CSRF token) to apply to later requests. */
	protected Session signIn(String email, String password) {
		return signIn(email, password, null);
	}

	/** Signs in, optionally while already carrying {@code existing}'s cookie (to observe session-id rotation). */
	protected Session signIn(String email, String password, Session existing) {
		Csrf csrf = csrf();
		ResponseSpec response = signin(email, password, existing, csrf);
		response.expectStatus().isOk();
		String sessionId = response.returnResult().getResponseHeaders().getOrEmpty(HttpHeaders.SET_COOKIE).stream()
				.filter(c -> c.startsWith("SESSION="))
				.map(c -> c.substring("SESSION=".length()).split(";", 2)[0])
				.findFirst().orElseThrow();
		return new Session(sessionId, csrf.token());
	}

	/** Raw sign-in call (any outcome); pass a prior {@link Session} to send its cookie, or null for none. */
	protected ResponseSpec signin(String email, String password, Session existing) {
		return signin(email, password, existing, csrf());
	}

	private ResponseSpec signin(String email, String password, Session existing, Csrf csrf) {
		return http.post().uri("/api/auth/signin")
				.headers(h -> {
					if (existing != null) {
						existing.apply(h);
					}
					else {
						csrf.apply(h);
					}
				})
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", email, "password", password))
				.exchange();
	}

	/**
	 * A signed-in browser session: {@code http.get().uri(...).headers(session::apply)}. Sends the SESSION cookie
	 * and a matching CSRF cookie/header, so it also works for POST/PUT/PATCH/DELETE.
	 */
	protected record Session(String sessionId, String csrfToken) {

		public void apply(HttpHeaders headers) {
			headers.set(HttpHeaders.COOKIE, "SESSION=" + sessionId + "; " + Csrf.COOKIE + "=" + csrfToken);
			headers.set(Csrf.HEADER, csrfToken);
		}

	}

	/**
	 * Fetches a CSRF token the way the frontend does (cookie-to-header). Apply it to any state-changing request
	 * with {@code .headers(csrf::apply)}.
	 */
	protected Csrf csrf() {
		List<String> setCookies = http.get().uri("/actuator/health")
				.exchange()
				.returnResult()
				.getResponseHeaders()
				.getOrEmpty(HttpHeaders.SET_COOKIE);
		String token = setCookies.stream()
				.filter(c -> c.startsWith(Csrf.COOKIE + "="))
				.map(c -> c.substring(Csrf.COOKIE.length() + 1).split(";", 2)[0])
				.findFirst()
				.orElse(null);
		assertThat(token).as("XSRF-TOKEN cookie").isNotBlank();
		return new Csrf(token);
	}

	/** A CSRF token, sent back as both the {@code XSRF-TOKEN} cookie and the {@code X-XSRF-TOKEN} header. */
	protected record Csrf(String token) {

		static final String COOKIE = "XSRF-TOKEN";

		static final String HEADER = "X-XSRF-TOKEN";

		public void apply(HttpHeaders headers) {
			headers.add(HttpHeaders.COOKIE, COOKIE + "=" + token);
			headers.set(HEADER, token);
		}

	}

}
