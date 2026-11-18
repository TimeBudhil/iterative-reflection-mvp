package iterative_reflection.mvp.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** Abandon and resurfacing, through HTTP [R4.7, R4.8, R6.7]. */
@Import(AbandonHttpTest.FailAfterAbandon.class)
class AbandonHttpTest extends FeedTestSupport {

	@Autowired
	FeedService feed;

	@Autowired
	FailAfterAbandon failure;

	@AfterEach
	void reset() {
		failure.fail.set(false);
	}

	/** Runs inside the abandon transaction after the penalty is written, so failing here proves rollback. */
	static class FailAfterAbandon {

		final AtomicBoolean fail = new AtomicBoolean();

		@EventListener
		void on(CareerAbandoned event) {
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

	ResponseSpec abandon(Session s, long career) {
		return http.post().uri("/api/careers/{id}/abandon", career).headers(s::apply).exchange();
	}

	@Test
	void abandonBeforeAnyPageViewedIs409() {
		Student st = student();
		long served = nextId(st.session());
		abandon(st.session(), served).expectStatus().isEqualTo(409);
		assertThat(nextId(st.session())).isEqualTo(served);
	}

	@Test
	void abandonAfterViewingClearsCardAndServesAnother() {
		Student st = student();
		long served = nextId(st.session());
		view(st.session(), served, 1).expectStatus().isOk();
		abandon(st.session(), served).expectStatus().isNoContent();
		assertThat(nextId(st.session())).isNotEqualTo(served);
	}


	double score(Student st, String tag) {
		return feed.topAffinities(st.id(), 50).stream().filter(t -> t.tagName().equals(tag)).findFirst().orElseThrow()
				.score();
	}

	@Test
	void abandonAppliesPenaltyToEachTagOfTheCareer() {
		Student st = student();
		long alpha = careerId("Fixture Career Alpha");
		view(st.session(), alpha, 2).expectStatus().isOk();
		abandon(st.session(), alpha).expectStatus().isNoContent();
		assertThat(score(st, "Fixture technology")).isEqualTo(-0.5);
		assertThat(score(st, "Fixture hands-on")).isEqualTo(-0.5);
		assertThat(feed.abandonedCareers(st.id())).singleElement().satisfies(a -> {
			assertThat(a.careerId()).isEqualTo(alpha);
			assertThat(a.title()).isEqualTo("Fixture Career Alpha");
			assertThat(a.lastViewedPage()).isEqualTo(2);
			assertThat(a.abandonCount()).isEqualTo(1);
		});
	}

	@Test
	void forcedFailureAfterPenaltyRollsBackTheAbandon() {
		Student st = student();
		long served = nextId(st.session());
		view(st.session(), served, 1).expectStatus().isOk();
		failure.fail.set(true);
		abandon(st.session(), served).expectStatus().is5xxServerError();
		failure.fail.set(false);
		assertThat(feed.abandonedCareers(st.id())).isEmpty();
		assertThat(feed.topAffinities(st.id(), 10)).isEmpty();
		assertThat(nextId(st.session())).isEqualTo(served);
	}

	@Test
	void abandonedCareersResurfaceOnlyAfterEveryUnseenOneThenAreGoneForGood() {
		Student st = student();
		int total = activeOrder().size();
		Set<Long> firstPass = new HashSet<>();
		for (int i = 0; i < total; i++) {
			long id = nextId(st.session());
			assertThat(firstPass.add(id)).as("career %d served again before all unseen ones", id).isTrue();
			view(st.session(), id, 1).expectStatus().isOk();
			abandon(st.session(), id).expectStatus().isNoContent();
		}
		assertThat(firstPass).containsExactlyInAnyOrderElementsOf(activeOrder());
		Set<Long> secondPass = new HashSet<>();
		for (int i = 0; i < total; i++) {
			Long id = nextId(st.session());
			assertThat(id).as("resurfaced career %d of %d", i + 1, total).isNotNull();
			assertThat(secondPass.add(id)).isTrue();
			abandon(st.session(), id).expectStatus().isNoContent();
		}
		assertThat(secondPass).isEqualTo(firstPass);
		assertThat(next(st.session()).expectStatus().isOk().returnResult(java.util.Map.class).getResponseBody())
				.containsEntry("exhausted", true).containsEntry("career", null);
		assertThat(feed.abandonedCareers(st.id())).hasSize(total).allMatch(a -> a.abandonCount() == 2);
		assertThat(score(st, "Fixture technology")).isEqualTo(-1.0);
		abandon(st.session(), activeOrder().get(0)).expectStatus().isEqualTo(409);
	}

	@Test
	void unknownCareerIs404AndNonStudentIs403() {
		Student st = student();
		abandon(st.session(), 987654321L).expectStatus().isNotFound();
		String email = createUser(uniqueEmail(), "correct horse battery", "COUNSELOR", false);
		abandon(signIn(email, "correct horse battery"), activeOrder().get(0)).expectStatus().isForbidden();
	}

}
