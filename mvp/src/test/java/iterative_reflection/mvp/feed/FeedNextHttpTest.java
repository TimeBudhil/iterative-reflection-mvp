package iterative_reflection.mvp.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FeedNextHttpTest extends FeedTestSupport {

	@Test
	void requiresSignIn() {
		http.get().uri("/api/feed/next").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void newStudentGetsFirstCareerInDefaultOrderWithCardFields() {
		Session s = signedInStudent();
		long first = activeOrder().get(0);
		next(s).expectStatus().isOk().expectBody()
				.jsonPath("$.exhausted").isEqualTo(false)
				.jsonPath("$.career.id").isEqualTo((int) first)
				.jsonPath("$.career.title").isNotEmpty()
				.jsonPath("$.career.summary").isNotEmpty()
				.jsonPath("$.career.estimatedMinutes").isNotEmpty()
				.jsonPath("$.career.tags").isArray()
				.jsonPath("$.career.lastPage").isNotEmpty();
	}

	@Test
	void servedCareerIsStoredAndSurvivesRepeatCallsAndSignInAgain() {
		String email = uniqueEmail();
		createUser(email, "correct horse battery", "STUDENT", false);
		Session s = signIn(email, "correct horse battery");
		Long first = nextId(s);
		assertThat(nextId(s)).isEqualTo(first);
		Session again = signIn(email, "correct horse battery");
		assertThat(nextId(again)).isEqualTo(first);
	}

	@Test
	void requestingOrStartingADifferentCareerWhileOneIsInProgressIs409() {
		Session s = signedInStudent();
		long served = nextId(s);
		long other = activeOrder().stream().filter(id -> id != served).findFirst().orElseThrow();
		http.get().uri("/api/careers/{id}/experience", other).headers(s::apply).exchange()
				.expectStatus().isEqualTo(409);
		view(s, other, 1).expectStatus().isEqualTo(409);
		http.get().uri("/api/careers/{id}/experience", served).headers(s::apply).exchange().expectStatus().isOk();
		view(s, served, 1).expectStatus().isOk();
		assertThat(nextId(s)).isEqualTo(served);
	}

	@Test
	void reflectingClearsInProgressAndNeverReappears() {
		Session s = signedInStudent();
		List<Long> order = activeOrder();
		long first = nextId(s);
		assertThat(first).isEqualTo(order.get(0));
		reflect(s, first, 7);
		long second = nextId(s);
		assertThat(second).isEqualTo(order.get(1));
		view(s, second, 1).expectStatus().isOk();
	}

	@Test
	void walksWholeActiveCatalogInOrderSkipsRetiredThenIsExhausted() {
		Session s = signedInStudent();
		List<Long> expected = activeOrder();
		List<Long> retired = jdbc.queryForList("select id from careers where active = false", Long.class);
		List<Long> served = new ArrayList<>();
		Long id;
		while ((id = nextId(s)) != null) {
			assertThat(served).doesNotContain(id);
			served.add(id);
			reflect(s, id, 6);
			assertThat(served.size()).isLessThanOrEqualTo(expected.size());
		}
		assertThat(served).isEqualTo(expected).doesNotContainAnyElementsOf(retired);
		next(s).expectStatus().isOk().expectBody()
				.jsonPath("$.career").isEmpty()
				.jsonPath("$.exhausted").isEqualTo(true);
	}

	@Test
	void careerRetiredWhileInProgressIsReplacedByNextActive() {
		Session s = signedInStudent();
		long first = nextId(s);
		jdbc.update("update careers set active = false where id = ?", first);
		try {
			assertThat(nextId(s)).isNotEqualTo(first);
		}
		finally {
			jdbc.update("update careers set active = true where id = ?", first);
		}
	}

	@Test
	void nonStudentsCannotUseTheFeed() {
		String email = uniqueEmail();
		createUser(email, "correct horse battery", "COUNSELOR", false);
		next(signIn(email, "correct horse battery")).expectStatus().isForbidden();
	}

}
