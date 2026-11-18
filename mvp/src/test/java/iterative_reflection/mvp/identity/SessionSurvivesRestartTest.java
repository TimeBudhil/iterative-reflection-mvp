package iterative_reflection.mvp.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.MvpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * A session lives in the database, not in application memory: a brand-new application instance (fresh session
 * repository, same database) still authenticates the cookie. The "restart" is a second context in the same JVM,
 * sharing the in-memory H2 database.
 */
class SessionSurvivesRestartTest extends HttpIntegrationTest {

	@Test
	void sessionCookieStillAuthenticatesOnANewApplicationInstance() {
		Session session = signedInStudent();
		try (ConfigurableApplicationContext second = new SpringApplicationBuilder(MvpApplication.class)
				.profiles("dev").properties("server.port=0", "spring.main.banner-mode=off").run()) {
			String port = second.getEnvironment().getProperty("local.server.port");
			RestClient client = RestClient.create("http://localhost:" + port);
			String body = client.get().uri("/api/me").header("Cookie", "SESSION=" + session.sessionId())
					.retrieve().body(String.class);
			assertThat(body).contains("\"email\"");
			assertThatThrownBy(() -> client.get().uri("/api/me").header("Cookie", "SESSION=bogus")
					.retrieve().body(String.class)).isInstanceOf(HttpClientErrorException.Unauthorized.class);
		}
	}

}
