package iterative_reflection.mvp.reflection;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ReflectionHttpTest extends ReflectionTestSupport {

	@Test
	void questionsAreSeededVersionedAndOrdered() {
		http.get().uri("/api/reflection-questions").headers(signedInStudent()::apply).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(5)
				.jsonPath("$[0].key").isEqualTo("interest")
				.jsonPath("$[1].key").isEqualTo("values")
				.jsonPath("$[2].key").isEqualTo("lifestyle")
				.jsonPath("$[3].key").isEqualTo("responsibilities")
				.jsonPath("$[4].key").isEqualTo("sacrifices")
				.jsonPath("$[0].prompt").isNotEmpty()
				.jsonPath("$[0].openPrompt").isNotEmpty()
				.jsonPath("$[0].minScore").isEqualTo(1)
				.jsonPath("$[0].maxScore").isEqualTo(10)
				.jsonPath("$[0].version").isEqualTo(1);
	}

	@Test
	void questionsRequireSignIn() {
		http.get().uri("/api/reflection-questions").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void submitBeforeLastPageViewedIs409() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 2);
		submit(s, c, body(answers(5), "YES")).expectStatus().isEqualTo(409);
	}

	@Test
	void validSubmitIsListedWithScoresAndTimestamps() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		Map<String, Object> a = answers(7);
		a.put("interest", Map.of("score", 9, "openText", "I loved it"));
		submit(s, c, body(a, "MAYBE")).expectStatus().isCreated().expectBody()
				.jsonPath("$.careerId").isEqualTo((int) c)
				.jsonPath("$.verdict").isEqualTo("MAYBE")
				.jsonPath("$.versionNumber").isEqualTo(1);
		list(s, "").expectStatus().isOk().expectBody()
				.jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.page.totalElements").isEqualTo(1)
				.jsonPath("$.content[0].careerId").isEqualTo((int) c)
				.jsonPath("$.content[0].careerTitle").isEqualTo("Fixture Career Alpha")
				.jsonPath("$.content[0].verdict").isEqualTo("MAYBE")
				.jsonPath("$.content[0].answers.length()").isEqualTo(5)
				.jsonPath("$.content[0].answers[0].key").isEqualTo("interest")
				.jsonPath("$.content[0].answers[0].score").isEqualTo(9)
				.jsonPath("$.content[0].answers[0].openText").isEqualTo("I loved it")
				.jsonPath("$.content[0].answers[1].score").isEqualTo(7)
				.jsonPath("$.content[0].createdAt").isNotEmpty()
				.jsonPath("$.content[0].updatedAt").isNotEmpty();
	}

	@Test
	void invalidScoresAndTextGive400WithPerFieldMessages() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		Map<String, Object> a = answers(5);
		a.put("interest", Map.of("score", 0));
		a.put("values", Map.of("score", 11));
		a.put("lifestyle", Map.of("score", 5, "openText", "x".repeat(1001)));
		submit(s, c, body(a, "YES")).expectStatus().isBadRequest().expectBody()
				.jsonPath("$.errors[?(@.field=='answers[interest].score')]").isNotEmpty()
				.jsonPath("$.errors[?(@.field=='answers[values].score')]").isNotEmpty()
				.jsonPath("$.errors[?(@.field=='answers[lifestyle].openText')]").isNotEmpty();
		list(s, "").expectBody().jsonPath("$.page.totalElements").isEqualTo(0);
	}

	@Test
	void missingQuestionAnswerIs400() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		Map<String, Object> a = answers(5);
		a.remove("sacrifices");
		submit(s, c, body(a, "YES")).expectStatus().isBadRequest().expectBody()
				.jsonPath("$.errors[?(@.field=='answers[sacrifices].score')]").isNotEmpty();
	}

	@Test
	void missingScoreInsideAnAnswerIs400() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		Map<String, Object> a = answers(5);
		a.put("interest", Map.of("openText", "no score"));
		submit(s, c, body(a, "YES")).expectStatus().isBadRequest().expectBody()
				.jsonPath("$.errors[?(@.field=='answers[interest].score')]").isNotEmpty();
	}

	@Test
	void missingVerdictIs400() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		submit(s, c, body(answers(5), null)).expectStatus().isBadRequest().expectBody()
				.jsonPath("$.errors[?(@.field=='verdict')]").isNotEmpty();
	}

	@Test
	void secondSubmitForSameCareerIs409AndNothingChanges() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		submit(s, c, body(answers(5), "YES")).expectStatus().isCreated();
		submit(s, c, body(answers(1), "NO")).expectStatus().isEqualTo(409);
		list(s, "").expectBody().jsonPath("$.content[0].verdict").isEqualTo("YES")
				.jsonPath("$.content[0].answers[0].score").isEqualTo(5);
	}

	@Test
	void unknownOrRetiredCareerIs404AndOnlyStudentsMaySubmit() {
		Session s = signedInStudent();
		submit(s, 999_999L, body(answers(5), "YES")).expectStatus().isNotFound();
		submit(s, careerId("Fixture Career Retired"), body(answers(5), "YES")).expectStatus().isNotFound();
		Session counselor = signIn(createUser(uniqueEmail(), "correct horse battery", "COUNSELOR", false),
				"correct horse battery");
		submit(counselor, careerId("Fixture Career Alpha"), body(answers(5), "YES")).expectStatus().isForbidden();
		list(counselor, "").expectStatus().isForbidden();
	}

	@Test
	void listIsNewestFirstPaginatedAndPerStudent() {
		Session s = signedInStudent();
		long alpha = careerId("Fixture Career Alpha");
		long beta = careerId("Fixture Career Beta");
		view(s, alpha, 3);
		submit(s, alpha, body(answers(5), "YES")).expectStatus().isCreated();
		view(s, beta, 2);
		submit(s, beta, body(answers(5), "NO")).expectStatus().isCreated();
		list(s, "?size=1").expectBody()
				.jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.content[0].careerTitle").isEqualTo("Fixture Career Beta")
				.jsonPath("$.page.totalElements").isEqualTo(2)
				.jsonPath("$.page.totalPages").isEqualTo(2);
		list(s, "?size=1&page=1").expectBody().jsonPath("$.content[0].careerTitle").isEqualTo("Fixture Career Alpha");
		list(signedInStudent(), "").expectBody().jsonPath("$.page.totalElements").isEqualTo(0);
	}

}
