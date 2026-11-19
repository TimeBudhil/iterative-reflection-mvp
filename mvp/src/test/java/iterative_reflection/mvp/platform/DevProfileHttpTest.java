package iterative_reflection.mvp.platform;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class DevProfileHttpTest extends HttpIntegrationTest {

	@Autowired
	Flyway flyway;

	@Test
	void flywayMigrationsAreAppliedOnStartup() {
		assertThat(Arrays.stream(flyway.info().applied()).map(MigrationInfo::getScript))
				.contains("V1__baseline.sql");
		assertThat(flyway.info().pending()).isEmpty();
	}

	@Test
	void healthIsPublic() {
		http.get().uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.expectBody().jsonPath("$.status").isEqualTo("UP")
				.jsonPath("$.components").doesNotExist();
	}

	@Test
	void prometheusRequiresAuthentication() {
		http.get().uri("/actuator/prometheus").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void otherActuatorEndpointsRequireAuthentication() {
		http.get().uri("/actuator").exchange().expectStatus().isUnauthorized();
		http.get().uri("/actuator/info").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void openApiDocumentIsServed() {
		http.get().uri("/v3/api-docs")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
				.expectBody().jsonPath("$.openapi").exists();
	}

	@Test
	void swaggerUiIsServed() {
		http.get().uri("/swagger-ui/index.html")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
	}

}
