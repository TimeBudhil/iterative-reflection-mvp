package iterative_reflection.mvp.feed;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Shared helpers for feed HTTP tests. The catalog may contain seed careers, so tests compare by order. */
abstract class FeedTestSupport extends HttpIntegrationTest {

	/** Ids of the active catalog in default order (independent source: SQL, not the feed). */
	List<Long> activeOrder() {
		return jdbc.queryForList("select id from careers where active = true order by default_order, id", Long.class);
	}

	long careerId(String title) {
		return jdbc.queryForObject("select id from careers where title = ?", Long.class, title);
	}

	ResponseSpec next(Session s) {
		return http.get().uri("/api/feed/next").headers(s::apply).exchange();
	}

	/** Serves the next card and returns its career id, or null when exhausted. */
	Long nextId(Session s) {
		Map<?, ?> body = next(s).expectStatus().isOk().returnResult(Map.class).getResponseBody();
		Map<?, ?> career = (Map<?, ?>) body.get("career");
		return career == null ? null : ((Number) career.get("id")).longValue();
	}

	int lastPage(long careerId) {
		return jdbc.queryForObject("select max(page_number) from career_pages where career_id = ?", Integer.class,
				careerId);
	}

	ResponseSpec view(Session s, long career, int page) {
		return http.post().uri("/api/careers/{id}/pages/{n}/viewed", career, page).headers(s::apply).exchange();
	}

	/** Views every page of the career and submits a reflection with the given score on all questions. */
	void reflect(Session s, long career, int score) {
		for (int p = 1; p <= lastPage(career); p++) {
			view(s, career, p).expectStatus().isOk();
		}
		Map<String, Object> answers = new LinkedHashMap<>();
		List.of("interest", "values", "lifestyle", "responsibilities", "sacrifices")
				.forEach(k -> answers.put(k, Map.of("score", score)));
		http.post().uri("/api/careers/{id}/reflection", career).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("answers", answers, "verdict", "YES")).exchange()
				.expectStatus().isCreated();
	}

}
