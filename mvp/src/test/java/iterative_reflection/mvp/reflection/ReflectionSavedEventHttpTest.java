package iterative_reflection.mvp.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

/** The "reflection saved" event is synchronous and in-transaction: a failing listener rolls the reflection back. */
@Import(ReflectionSavedEventHttpTest.Probe.class)
class ReflectionSavedEventHttpTest extends ReflectionTestSupport {

	@Autowired
	Probe probe;

	@AfterEach
	void reset() {
		probe.fail.set(false);
		probe.seen.clear();
	}

	@Test
	void eventCarriesStudentCareerMeanAndReflectionId() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		Map<String, Object> a = answers(6);
		a.put("interest", Map.of("score", 10));
		submit(s, c, body(a, "YES")).expectStatus().isCreated();
		assertThat(probe.seen).hasSize(1);
		ReflectionSaved e = probe.seen.get(0);
		assertThat(e.careerId()).isEqualTo(c);
		assertThat(e.scaleMean()).isEqualTo(6.8);
		assertThat(e.reflectionId()).isPositive();
		assertThat(e.studentId()).isPositive();
	}

	@Test
	void failingListenerRollsTheReflectionBack() {
		Session s = signedInStudent();
		long c = careerId("Fixture Career Alpha");
		view(s, c, 3);
		probe.fail.set(true);
		submit(s, c, body(answers(6), "YES")).expectStatus().is5xxServerError();
		list(s, "").expectBody().jsonPath("$.page.totalElements").isEqualTo(0);
		probe.fail.set(false);
		submit(s, c, body(answers(6), "YES")).expectStatus().isCreated();
	}

	static class Probe {

		final List<ReflectionSaved> seen = new CopyOnWriteArrayList<>();

		final AtomicBoolean fail = new AtomicBoolean();

		@EventListener
		void on(ReflectionSaved event) {
			seen.add(event);
			if (fail.get()) {
				throw new IllegalStateException("boom");
			}
		}

	}

}
