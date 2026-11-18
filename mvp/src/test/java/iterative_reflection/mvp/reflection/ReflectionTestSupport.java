package iterative_reflection.mvp.reflection;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Shared arrange/act helpers for reflection HTTP tests. */
abstract class ReflectionTestSupport extends HttpIntegrationTest {

	static final List<String> KEYS = List.of("interest", "values", "lifestyle", "responsibilities", "sacrifices");

	long careerId(String title) {
		return jdbc.queryForObject("select id from careers where title = ?", Long.class, title);
	}

	/** An answer map covering every active question with the given score. */
	Map<String, Object> answers(int score) {
		Map<String, Object> m = new LinkedHashMap<>();
		KEYS.forEach(k -> m.put(k, Map.of("score", score)));
		return m;
	}

	Map<String, Object> body(Map<String, Object> answers, String verdict) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("answers", answers);
		if (verdict != null) {
			m.put("verdict", verdict);
		}
		return m;
	}

	void view(Session s, long career, int pages) {
		for (int p = 1; p <= pages; p++) {
			http.post().uri("/api/careers/{id}/pages/{n}/viewed", career, p).headers(s::apply).exchange()
					.expectStatus().isOk();
		}
	}

	ResponseSpec submit(Session s, long career, Object body) {
		return http.post().uri("/api/careers/{id}/reflection", career).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	ResponseSpec list(Session s, String query) {
		return http.get().uri("/api/students/me/reflections" + query).headers(s::apply).exchange();
	}

}
