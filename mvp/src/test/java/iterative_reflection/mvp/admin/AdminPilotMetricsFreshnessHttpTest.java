package iterative_reflection.mvp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Only the Prometheus gauges are served from the metrics cache; the admin endpoint always computes fresh. */
@TestPropertySource(properties = "pilot.metrics.cache-ttl=PT1H")
class AdminPilotMetricsFreshnessHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	private long totalStudents(Session admin) {
		Map<?, ?> body = http.get().uri("/api/admin/pilot-metrics").headers(admin::apply).exchange().expectStatus()
				.isOk().returnResult(Map.class).getResponseBody();
		return ((Number) body.get("totalStudents")).longValue();
	}

	@Test
	void endpointReflectsNewStudentsEvenWithALongCacheTtl() {
		Session admin = signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
		long before = totalStudents(admin);
		signedInStudent();
		assertThat(totalStudents(admin)).isEqualTo(before + 1);
	}

}
