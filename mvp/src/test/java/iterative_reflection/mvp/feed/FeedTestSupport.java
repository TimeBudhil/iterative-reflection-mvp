package iterative_reflection.mvp.feed;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import java.util.Map;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Shared helpers for feed HTTP tests. The catalog may contain seed careers, so tests compare by order. */
abstract class FeedTestSupport extends HttpIntegrationTest {

	/** Ids of the active catalog in default order (independent source: SQL, not the feed). */
	List<Long> activeOrder() {
		return jdbc.queryForList("select id from careers where active = true order by default_order, id", Long.class);
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

}
