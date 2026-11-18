package iterative_reflection.mvp.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.MvpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "app.admin.email=Boot.Admin@Test.Example",
		"app.admin.password=bootstrap admin passphrase" })
class BootstrapAdminHttpTest extends HttpIntegrationTest {

	private static final String EMAIL = "boot.admin@test.example";

	private static final String PASSWORD = "bootstrap admin passphrase";

	@Test
	void adminIsCreatedOnceAndCanSignInWithoutPasswordChange() {
		Session s = signIn(EMAIL, PASSWORD);
		http.get().uri("/api/me").headers(s::apply).exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.role").isEqualTo("ADMIN").jsonPath("$.mustChangePassword").isEqualTo(false);
		http.get().uri("/actuator/prometheus").headers(s::apply).exchange().expectStatus().isOk();
	}

	@Test
	void restartDoesNotDuplicateTheAdmin() {
		// a second application context on the same (shared in-memory) database is a restart
		try (ConfigurableApplicationContext second = new SpringApplicationBuilder(MvpApplication.class)
				.web(WebApplicationType.SERVLET).profiles("dev")
				.run("--app.admin.email=" + EMAIL, "--app.admin.password=" + PASSWORD, "--server.port=0")) {
			assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, EMAIL))
					.isEqualTo(1);
		}
	}

	@Test
	void startupFailsWhenAdminPasswordIsTooShort() {
		assertThatThrownBy(() -> new SpringApplicationBuilder(MvpApplication.class).web(WebApplicationType.SERVLET)
				.profiles("dev")
				.run("--app.admin.email=other.admin@test.example", "--app.admin.password=short", "--server.port=0")
				.close())
				.hasStackTraceContaining("ADMIN_PASSWORD");
	}

}
