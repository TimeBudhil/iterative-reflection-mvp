package iterative_reflection.mvp.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * The {@code prod} profile takes its database from the {@code DB_URL}, {@code DB_USERNAME} and {@code DB_PASSWORD}
 * environment variables. Here they point at an H2 database in PostgreSQL mode, standing in for Postgres.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"DB_URL=jdbc:h2:mem:prod-profile-check;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
		"DB_USERNAME=prod_user",
		"DB_PASSWORD=prod_secret" })
@AutoConfigureRestTestClient
@AutoConfigureMetrics
@ActiveProfiles("prod")
class ProdProfileHttpTest {

	@Autowired
	RestTestClient http;

	@Autowired
	DataSource dataSource;

	@Test
	void databaseConnectionComesFromEnvironmentVariables() {
		HikariDataSource hikari = (HikariDataSource) dataSource;
		assertThat(hikari.getJdbcUrl()).startsWith("jdbc:h2:mem:prod-profile-check");
		assertThat(hikari.getUsername()).isEqualTo("prod_user");
	}

	@Test
	void healthIsPublic() {
		http.get().uri("/actuator/health").exchange().expectStatus().isOk();
	}

	@Test
	void onlyHealthAndPrometheusAreExposed() {
		http.get().uri("/actuator/prometheus").exchange().expectStatus().isUnauthorized();
		http.get().uri("/actuator/info").exchange().expectStatus().isNotFound();
	}

	@Test
	void apiDocsAndSwaggerUiAreNotServed() {
		http.get().uri("/v3/api-docs").exchange().expectStatus().isNotFound();
		http.get().uri("/swagger-ui/index.html").exchange().expectStatus().isNotFound();
	}

}
