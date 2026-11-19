package iterative_reflection.mvp.counselor;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.MvpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;

/** The dev demo seed is opt-in (tests turn it off in the base class); this class opts in. */
@TestPropertySource(properties = "app.demo-seed.enabled=true")
class DemoSeedHttpTest extends HttpIntegrationTest {

	@Test
	void demoAccountsExistLinkedAndSignInWithDemoPassword() {
		assertThat(demoCount()).isEqualTo(4);
		Session student = signIn("student1@demo.example", "demo");
		http.get().uri("/api/students/me/counselors").headers(student::apply).exchange().expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(1)
				.jsonPath("$.content[0].displayName").isEqualTo("Demo Counselor");
		signIn("student2@demo.example", "demo");
		signIn("counselor@demo.example", "demo");
		signIn("admin@demo.example", "demo");
		assertThat(demoLinks()).isEqualTo(2);
	}

	@Test
	void restartDoesNotDuplicate() {
		try (ConfigurableApplicationContext second = new SpringApplicationBuilder(MvpApplication.class)
				.run("--spring.profiles.active=dev", "--app.demo-seed.enabled=true", "--server.port=0")) {
			assertThat(second).isNotNull();
		}
		assertThat(demoCount()).isEqualTo(4);
		assertThat(demoLinks()).isEqualTo(2);
	}

	private long demoLinks() {
		return jdbc.queryForObject(
				"select count(*) from counselor_links where student_id in (select id from users where demo = true)",
				Long.class);
	}

	private long demoCount() {
		return jdbc.queryForObject("select count(*) from users where demo = true", Long.class);
	}

}
