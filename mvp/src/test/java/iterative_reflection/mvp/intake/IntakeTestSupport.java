package iterative_reflection.mvp.intake;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Shared helpers for intake HTTP tests. */
abstract class IntakeTestSupport extends HttpIntegrationTest {

	record Student(Session session, long id) {
	}

	Student student() {
		String email = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email.toLowerCase());
		return new Student(signIn(email, "correct horse battery"), id);
	}

	long tagId(String name) {
		return jdbc.queryForObject("select id from tags where name = ?", Long.class, name);
	}

	/** Reflects (all pages viewed, all scores 8) on the given careers. */
	void reflectOn(Session s, long... careers) {
		for (long career : careers) {
			int last = jdbc.queryForObject("select max(page_number) from career_pages where career_id = ?",
					Integer.class, career);
			for (int p = 1; p <= last; p++) {
				http.post().uri("/api/careers/{id}/pages/{n}/viewed", career, p).headers(s::apply).exchange()
						.expectStatus().isOk();
			}
			Map<String, Object> answers = new LinkedHashMap<>();
			List.of("interest", "values", "lifestyle", "responsibilities", "sacrifices")
					.forEach(k -> answers.put(k, Map.of("score", 8)));
			http.post().uri("/api/careers/{id}/reflection", career).headers(s::apply)
					.contentType(MediaType.APPLICATION_JSON).body(Map.of("answers", answers, "verdict", "YES"))
					.exchange().expectStatus().isCreated();
		}
	}

	/** Three active careers (two fixtures plus the first seed career), in a fixed order. */
	List<Long> threeCareers() {
		return jdbc.queryForList("select id from careers where active = true order by default_order, id limit 3",
				Long.class);
	}

	void reflectThree(Session s) {
		reflectOn(s, threeCareers().stream().mapToLong(Long::longValue).toArray());
	}

	ResponseSpec post(Session s, Object body) {
		return http.post().uri("/api/intake").headers(s::apply).contentType(MediaType.APPLICATION_JSON).body(body)
				.exchange();
	}

	ResponseSpec put(Session s, Object body) {
		return http.put().uri("/api/intake").headers(s::apply).contentType(MediaType.APPLICATION_JSON).body(body)
				.exchange();
	}

	ResponseSpec dismiss(Session s) {
		return http.post().uri("/api/intake/dismiss").headers(s::apply).exchange();
	}

	boolean promptDue(Session s) {
		Map<?, ?> me = http.get().uri("/api/me").headers(s::apply).exchange().expectStatus().isOk()
				.returnResult(Map.class).getResponseBody();
		return (Boolean) me.get("intakePromptDue");
	}

	Map<String, Object> interests(Long... tagIds) {
		return Map.of("answers", Map.of("interests", Map.of("tagIds", List.of(tagIds))));
	}

}
