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

	Map<String, Object> body(Map<String, Object> answers, String verdict) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("answers", answers);
		if (verdict != null) {
			m.put("verdict", verdict);
		}
		return m;
	}

	ResponseSpec submit(Session s, long career, Object body) {
		return http.post().uri("/api/careers/{id}/reflection", career).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	ResponseSpec list(Session s, String query) {
		return http.get().uri("/api/students/me/reflections" + query).headers(s::apply).exchange();
	}

}
