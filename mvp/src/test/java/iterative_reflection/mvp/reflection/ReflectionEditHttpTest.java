package iterative_reflection.mvp.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.feed.TagAffinity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class ReflectionEditHttpTest extends ReflectionTestSupport {

	@Autowired
	FeedService feed;

	ResponseSpec edit(Session s, long career, Object body) {
		return http.put().uri("/api/careers/{id}/reflection", career).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	ResponseSpec history(Session s, long career, String query) {
		return http.get().uri("/api/careers/{id}/reflection/versions" + query, career).headers(s::apply).exchange();
	}

	record Reflected(Session session, long studentId, long career) {
	}

	Reflected reflected(int score, String verdict) {
		String email = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email.toLowerCase());
		Session s = signIn(email, "correct horse battery");
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		submit(s, c, body(answers(score), verdict)).expectStatus().isCreated();
		return new Reflected(s, id, c);
	}

	@Test
	void editReplacesCurrentAnswersAndBumpsVersion() {
		Reflected r = reflected(5, "MAYBE");
		Map<String, Object> a = answers(8);
		a.put("interest", Map.of("score", 2, "openText", "changed my mind"));
		edit(r.session(), r.career(), body(a, "NO")).expectStatus().isOk().expectBody()
				.jsonPath("$.versionNumber").isEqualTo(2)
				.jsonPath("$.verdict").isEqualTo("NO");
		list(r.session(), "").expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(1)
				.jsonPath("$.content[0].versionNumber").isEqualTo(2)
				.jsonPath("$.content[0].verdict").isEqualTo("NO")
				.jsonPath("$.content[0].answers[0].score").isEqualTo(2)
				.jsonPath("$.content[0].answers[0].openText").isEqualTo("changed my mind")
				.jsonPath("$.content[0].answers[1].score").isEqualTo(8);
	}

	@Test
	void editWithoutExistingReflectionIs404() {
		Session s = signedInStudent();
		edit(s, careerId("Fixture Career Alpha"), body(answers(5), "YES")).expectStatus().isNotFound();
	}

	@Test
	void invalidEditIs400AndLeavesReflectionUntouched() {
		Reflected r = reflected(5, "YES");
		Map<String, Object> a = answers(5);
		a.put("interest", Map.of("score", 11));
		edit(r.session(), r.career(), body(a, "YES")).expectStatus().isBadRequest();
		list(r.session(), "").expectBody().jsonPath("$.content[0].versionNumber").isEqualTo(1);
	}

	@Test
	void historyListsEveryVersionNewestFirstWithTimestampsAndIsPaginated() {
		Reflected r = reflected(5, "MAYBE");
		edit(r.session(), r.career(), body(answers(6), "YES")).expectStatus().isOk();
		edit(r.session(), r.career(), body(answers(7), "NO")).expectStatus().isOk();
		history(r.session(), r.career(), "").expectStatus().isOk().expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(3)
				.jsonPath("$.content[0].versionNumber").isEqualTo(3)
				.jsonPath("$.content[0].verdict").isEqualTo("NO")
				.jsonPath("$.content[0].createdAt").isNotEmpty()
				.jsonPath("$.content[0].answers[0].score").isEqualTo(7)
				.jsonPath("$.content[2].versionNumber").isEqualTo(1)
				.jsonPath("$.content[2].verdict").isEqualTo("MAYBE")
				.jsonPath("$.content[2].answers[0].score").isEqualTo(5);
		history(r.session(), r.career(), "?size=2&page=1").expectBody()
				.jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.content[0].versionNumber").isEqualTo(1);
	}

	@Test
	void historyIsOnlyVisibleToItsOwner() {
		Reflected r = reflected(5, "YES");
		history(signedInStudent(), r.career(), "").expectStatus().isNotFound();
	}

	@Test
	void flippingAllTensToAllOnesMovesTagsFromPlusOneToMinusOne() {
		Reflected r = reflected(10, "YES");
		assertThat(feed.topAffinities(r.studentId(), 10)).extracting(TagAffinity::score).containsOnly(1.0);
		edit(r.session(), r.career(), body(answers(1), "NO")).expectStatus().isOk();
		List<TagAffinity> top = feed.topAffinities(r.studentId(), 10);
		assertThat(top).hasSize(2);
		assertThat(top).extracting(TagAffinity::score).containsOnly(-1.0);
	}

	@Test
	void editUsesTheCurrentlyActiveQuestionSetAndOldVersionsKeepTheirOwn() {
		Reflected r = reflected(5, "YES");
		jdbc.update("update reflection_questions set active = false where question_key = 'interest'");
		jdbc.update("""
				insert into reflection_questions (question_key, version, prompt, open_prompt, display_order, active)
				values ('interest', 2, 'Reworded interest?', null, 1, true)""");
		try {
			edit(r.session(), r.career(), body(answers(9), "YES")).expectStatus().isOk();
			history(r.session(), r.career(), "").expectBody()
					.jsonPath("$.content[0].answers[0].questionVersion").isEqualTo(2)
					.jsonPath("$.content[0].answers[0].prompt").isEqualTo("Reworded interest?")
					.jsonPath("$.content[1].answers[0].questionVersion").isEqualTo(1);
		}
		finally {
			jdbc.update("""
					delete from reflection_answers where question_id in
					(select id from reflection_questions where question_key = 'interest' and version = 2)""");
			jdbc.update("delete from reflection_questions where question_key = 'interest' and version = 2");
			jdbc.update("update reflection_questions set active = true where question_key = 'interest' and version = 1");
		}
	}

	@Test
	void noEndpointDeletesReflectionsOrHistory() {
		Reflected r = reflected(5, "YES");
		http.delete().uri("/api/careers/{id}/reflection", r.career()).headers(r.session()::apply).exchange()
				.expectStatus().value(s -> assertThat(s).isIn(404, 405));
		http.delete().uri("/api/careers/{id}/reflection/versions", r.career()).headers(r.session()::apply).exchange()
				.expectStatus().value(s -> assertThat(s).isIn(404, 405));
		list(r.session(), "").expectBody().jsonPath("$.page.totalElements").isEqualTo(1);
	}

}
