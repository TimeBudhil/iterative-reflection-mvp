package iterative_reflection.mvp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

	@Autowired
	protected RestTestClient http;

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
