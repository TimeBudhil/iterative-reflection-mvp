package iterative_reflection.mvp.platform;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Import(ErrorHandlingHttpTest.ProbeController.class)
class ErrorHandlingHttpTest extends HttpIntegrationTest {

	@Test
	void unknownRouteReturns404AsProblemJson() {
		http.get().uri("/api/does-not-exist")
				.headers(signedInStudent()::apply)
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(404)
				.jsonPath("$.title").isEqualTo("Not Found")
				.jsonPath("$.trace").doesNotExist();
	}

	@Test
	void validationFailureListsEachFailingField() {
		Session session = signedInStudent();
		http.post().uri("/api/test-probe/widgets")
				.headers(session::apply)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"name\": \" \", \"score\": 11}")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.status").isEqualTo(400)
				.jsonPath("$.errors.length()").isEqualTo(2)
				.jsonPath("$.errors[?(@.field == 'name')].message").exists()
				.jsonPath("$.errors[?(@.field == 'score')].message").exists();
	}

	@Test
	void malformedJsonReturns400ProblemWithoutParserInternals() {
		Session session = signedInStudent();
		String body = http.post().uri("/api/test-probe/widgets")
				.headers(session::apply)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"name\": ")
				.exchange()
				.expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody(String.class).returnResult().getResponseBody();
		assertNoInternals(body);
		assertThat(body).doesNotContain("jackson");
	}

	@Test
	void unexpectedFailureReturns500ProblemWithoutStackTraceOrSql() {
		String body = http.get().uri("/api/test-probe/explode")
				.headers(signedInStudent()::apply)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody(String.class).returnResult().getResponseBody();
		assertNoInternals(body);
	}

	@Test
	void unauthenticatedAccessToProtectedRouteIs401ProblemJson() {
		http.get().uri("/actuator/prometheus")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody().jsonPath("$.status").isEqualTo(401);
	}

	private static void assertNoInternals(String body) {
		assertThat(body)
				.doesNotContain("SELECT", "select ", "SQLException", "Exception", "\tat ", "iterative_reflection");
	}

	@RestController
	static class ProbeController {

		record Widget(@NotBlank String name, @Min(1) @Max(10) int score) {
		}

		@PostMapping("/api/test-probe/widgets")
		Widget create(@Valid @RequestBody Widget widget) {
			return widget;
		}

		@GetMapping("/api/test-probe/explode")
		String explode() throws SQLException {
			throw new SQLException("SELECT secret FROM users WHERE id = 1");
		}

	}

}
