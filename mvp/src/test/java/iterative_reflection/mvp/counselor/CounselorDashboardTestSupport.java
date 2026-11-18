package iterative_reflection.mvp.counselor;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Arrange/act helpers for counselor dashboard tests. */
abstract class CounselorDashboardTestSupport extends HttpIntegrationTest {

	static final String PASSWORD = "correct horse battery";

	@Autowired
	protected CounselorService counselors;

	record Counselor(long id, String code, Session session) {
	}

	record Student(long id, Session session) {
	}

	Counselor newCounselor() {
		String email = createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		String code = counselors.createProfile(id).joinCode();
		return new Counselor(id, code, signIn(email, PASSWORD));
	}

	Student newStudent() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		return new Student(id, signIn(email, PASSWORD));
	}

	Student linkedStudent(Counselor c) {
		Student s = newStudent();
		counselors.link(s.id(), c.code());
		return s;
	}

	/** The n-th active career in default order (0-based). */
	long career(int n) {
		return jdbc.queryForList("select id from careers where active = true order by default_order, id limit 10",
				Long.class).get(n);
	}

	int pages(long career) {
		return jdbc.queryForObject("select max(page_number) from career_pages where career_id = ?", Integer.class,
				career);
	}

	void view(Student s, long career, int pages) {
		for (int p = 1; p <= pages; p++) {
			http.post().uri("/api/careers/{id}/pages/{n}/viewed", career, p).headers(s.session()::apply).exchange()
					.expectStatus().isOk();
		}
	}

	void reflect(Student s, long career, int score, String verdict) {
		view(s, career, pages(career));
		Map<String, Object> answers = new LinkedHashMap<>();
		List.of("interest", "values", "lifestyle", "responsibilities", "sacrifices")
				.forEach(k -> answers.put(k, Map.of("score", score, "openText", "because " + k)));
		http.post().uri("/api/careers/{id}/reflection", career).headers(s.session()::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("answers", answers, "verdict", verdict))
				.exchange().expectStatus().isCreated();
	}

	ResponseSpec list(Counselor c, String query) {
		return http.get().uri("/api/counselor/students" + query).headers(c.session()::apply).exchange();
	}

	ResponseSpec detail(Counselor c, long studentId) {
		return http.get().uri("/api/counselor/students/{id}", studentId).headers(c.session()::apply).exchange();
	}

}
