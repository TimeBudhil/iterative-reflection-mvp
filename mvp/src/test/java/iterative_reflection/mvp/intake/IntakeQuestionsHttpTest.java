package iterative_reflection.mvp.intake;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

/** [R7.2] */
class IntakeQuestionsHttpTest extends IntakeTestSupport {

	@Test
	void returnsSeededQuestionsWithTagOptionsForInterests() {
		Student st = student();
		List<Map<String, Object>> qs = http.get().uri("/api/intake/questions").headers(st.session()::apply)
				.exchange().expectStatus().isOk()
				.returnResult(new ParameterizedTypeReference<List<Map<String, Object>>>() {
				}).getResponseBody();
		assertThat(qs).extracting(q -> q.get("key")).containsSubsequence("interests", "subjects", "goals", "why",
				"expectations");
		Map<String, Object> interests = qs.get(0);
		assertThat(interests.get("type")).isEqualTo("TAGS");
		assertThat((List<?>) interests.get("options")).isNotEmpty();
		assertThat(qs.get(1).get("type")).isEqualTo("TEXT");
	}

	@Test
	void newlyInsertedQuestionAppearsWithoutCodeChange() {
		jdbc.update("insert into intake_questions (question_key, prompt, question_type, display_order, active) "
				+ "values ('zz_test_q', 'Anything else?', 'TEXT', 9999, true)");
		try {
			Student st = student();
			String body = http.get().uri("/api/intake/questions").headers(st.session()::apply).exchange()
					.expectStatus().isOk().returnResult(String.class).getResponseBody();
			assertThat(body).contains("zz_test_q");
		}
		finally {
			jdbc.update("delete from intake_questions where question_key = 'zz_test_q'");
		}
	}

	@Test
	void requiresSignIn() {
		http.get().uri("/api/intake/questions").exchange().expectStatus().isUnauthorized();
	}

}
