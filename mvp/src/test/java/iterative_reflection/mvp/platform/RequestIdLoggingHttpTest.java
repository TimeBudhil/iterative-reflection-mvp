package iterative_reflection.mvp.platform;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class RequestIdLoggingHttpTest extends HttpIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	void responseCarriesAGeneratedRequestId() {
		String requestId = http.get().uri("/actuator/health")
				.exchange()
				.expectStatus().isOk()
				.returnResult()
				.getResponseHeaders()
				.getFirst("X-Request-Id");
		assertThat(requestId).isNotBlank();
	}

	@Test
	void wellFormedIncomingRequestIdIsEchoed() {
		http.get().uri("/actuator/health")
				.header("X-Request-Id", "client-abc-123")
				.exchange()
				.expectHeader().valueEquals("X-Request-Id", "client-abc-123");
	}

	@Test
	void malformedIncomingRequestIdIsReplaced() {
		String requestId = http.get().uri("/actuator/health")
				.header("X-Request-Id", "bad id\" injected")
				.exchange()
				.returnResult()
				.getResponseHeaders()
				.getFirst("X-Request-Id");
		assertThat(requestId).isNotBlank().doesNotContain(" ", "\"");
	}

	@Test
	void errorResponsesCarryTheRequestIdToo() {
		http.get().uri("/actuator/prometheus")
				.header("X-Request-Id", "denied-req-1")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().valueEquals("X-Request-Id", "denied-req-1");
	}

	@Test
	void logLinesAreJsonAndCarryTheRequestId(CapturedOutput output) {
		http.get().uri("/api/does-not-exist")
				.headers(signedInStudent()::apply)
				.header("X-Request-Id", "log-check-42")
				.exchange()
				.expectStatus().isNotFound();

		List<String> lines = output.getOut().lines().filter(line -> !line.isBlank()).toList();
		assertThat(lines).isNotEmpty();
		List<JsonNode> entries = lines.stream().map(JSON::readTree).toList();
		assertThat(entries)
				.anySatisfy(entry -> assertThat(entry.path("requestId").asString()).isEqualTo("log-check-42"));
	}

}
