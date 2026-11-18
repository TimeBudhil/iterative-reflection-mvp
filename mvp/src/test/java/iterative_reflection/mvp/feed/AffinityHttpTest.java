package iterative_reflection.mvp.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import iterative_reflection.mvp.reflection.ReflectionSaved;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Affinity contributions written by reflections, through HTTP, with default properties [R6.1-R6.5, N3.4]. */
@Import(AffinityHttpTest.FailAfterAffinity.class)
class AffinityHttpTest extends FeedTestSupport {

	@Autowired
	FeedService feed;

	@Autowired
	FailAfterAffinity failure;

	@AfterEach
	void reset() {
		failure.fail.set(false);
	}

	/** Runs after the feed's own listener, so a failure here proves the affinity rows are rolled back too. */
	static class FailAfterAffinity {

		final AtomicBoolean fail = new AtomicBoolean();

		@EventListener
		@Order(Integer.MAX_VALUE)
		void on(ReflectionSaved event) {
			if (fail.get()) {
				throw new IllegalStateException("boom");
			}
		}

	}

	record Student(Session session, long id) {
	}

	Student student() {
		String email = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email.toLowerCase());
		return new Student(signIn(email, "correct horse battery"), id);
	}

	double score(List<TagAffinity> top, String tagName) {
		return top.stream().filter(t -> t.tagName().equals(tagName)).findFirst().orElseThrow().score();
	}

	@Test
	void newStudentHasNoAffinity() {
		assertThat(feed.topAffinities(student().id(), 10)).isEmpty();
	}

	@Test
	void allTensAddPlusOneToEveryTagOfTheCareer() {
		Student st = student();
		reflect(st.session(), careerId("Fixture Career Alpha"), 10);
		List<TagAffinity> top = feed.topAffinities(st.id(), 10);
		assertThat(top).hasSize(2);
		assertThat(score(top, "Fixture technology")).isEqualTo(1.0);
		assertThat(score(top, "Fixture hands-on")).isEqualTo(1.0);
	}

	@Test
	void allOnesSubtractOneAndTopNOrdersHighestFirst() {
		Student st = student();
		reflect(st.session(), careerId("Fixture Career Alpha"), 1);
		reflect(st.session(), careerId("Fixture Career Beta"), 10);
		List<TagAffinity> top = feed.topAffinities(st.id(), 10);
		assertThat(top).extracting(TagAffinity::tagName).containsExactly("Fixture health", "Fixture technology",
				"Fixture hands-on");
		assertThat(top.get(0).score()).isEqualTo(1.0);
		assertThat(top.get(1).score()).isEqualTo(-1.0);
		assertThat(top.get(0).category()).isEqualTo("Fixture cluster");
		assertThat(feed.topAffinities(st.id(), 1)).hasSize(1);
	}

	@Test
	void verdictDoesNotAffectAffinity() {
		Student st = student();
		long beta = careerId("Fixture Career Beta");
		for (int p = 1; p <= lastPage(beta); p++) {
			view(st.session(), beta, p).expectStatus().isOk();
		}
		submit(st.session(), beta, "NO", 10).expectStatus().isCreated();
		assertThat(score(feed.topAffinities(st.id(), 5), "Fixture health")).isEqualTo(1.0);
	}

	@Test
	void forcedFailureAfterAffinityUpdateLeavesNothingSaved() {
		Student st = student();
		long alpha = careerId("Fixture Career Alpha");
		for (int p = 1; p <= 3; p++) {
			view(st.session(), alpha, p).expectStatus().isOk();
		}
		failure.fail.set(true);
		submit(st.session(), alpha, "YES", 8).expectStatus().is5xxServerError();
		assertThat(jdbc.queryForObject("select count(*) from affinity_contributions where student_id = ?",
				Integer.class, st.id())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from reflections where student_id = ?", Integer.class,
				st.id())).isZero();
		failure.fail.set(false);
		submit(st.session(), alpha, "YES", 8).expectStatus().isCreated();
		assertThat(score(feed.topAffinities(st.id(), 5), "Fixture technology")).isCloseTo(2.5 / 4.5, within(1e-9));
	}

	@Test
	void concurrentSubmitsYieldOneReflectionAndOneContributionSet() throws Exception {
		Student st = student();
		long alpha = careerId("Fixture Career Alpha");
		for (int p = 1; p <= 3; p++) {
			view(st.session(), alpha, p).expectStatus().isOk();
		}
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Callable<Integer> task = () -> submit(st.session(), alpha, "YES", 9).returnResult(String.class)
					.getStatus().value();
			Future<Integer> a = pool.submit(task);
			Future<Integer> b = pool.submit(task);
			assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(201, 409);
		}
		finally {
			pool.shutdownNow();
		}
		assertThat(jdbc.queryForObject("select count(*) from reflections where student_id = ?", Integer.class,
				st.id())).isEqualTo(1);
		assertThat(jdbc.queryForObject("select count(*) from affinity_contributions where student_id = ?",
				Integer.class, st.id())).isEqualTo(2);
	}

	/** Submits a reflection with the same score on all five questions (pages must already be viewed). */
	ResponseSpec submit(Session s, long career, String verdict, int score) {
		Map<String, Object> answers = new LinkedHashMap<>();
		List.of("interest", "values", "lifestyle", "responsibilities", "sacrifices")
				.forEach(k -> answers.put(k, Map.of("score", score)));
		return http.post().uri("/api/careers/{id}/reflection", career).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("answers", answers, "verdict", verdict))
				.exchange();
	}

}
