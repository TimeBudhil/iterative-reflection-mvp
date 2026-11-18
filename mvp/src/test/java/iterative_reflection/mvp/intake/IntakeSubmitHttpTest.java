package iterative_reflection.mvp.intake;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.feed.TagAffinity;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/** [R7.1, R7.3, R7.4, R6.4, N3.2] */
@Import(IntakeSubmitHttpTest.FailAfterAffinity.class)
class IntakeSubmitHttpTest extends IntakeTestSupport {

	@Autowired
	FeedService feed;

	@Autowired
	FailAfterAffinity failure;

	@AfterEach
	void reset() {
		failure.fail.set(false);
	}

	/** Runs after the feed's own listener, so a failure here proves the affinity rows roll back too. */
	static class FailAfterAffinity {

		final AtomicBoolean fail = new AtomicBoolean();

		@EventListener
		@Order(Integer.MAX_VALUE)
		void on(IntakeSaved event) {
			if (fail.get()) {
				throw new IllegalStateException("boom");
			}
		}

	}

	double score(long studentId, String tag) {
		return feed.topAffinities(studentId, 100).stream().filter(t -> t.tagName().equals(tag)).mapToDouble(
				TagAffinity::score).findFirst().orElse(0.0);
	}

	long intakeRows(long studentId) {
		return jdbc.queryForObject("select count(*) from intake_responses where student_id = ?", Long.class,
				studentId);
	}

	@Test
	void promptNotDueBeforeThreeReflections() {
		Student st = student();
		assertThat(promptDue(st.session())).isFalse();
		reflectOn(st.session(), threeCareers().get(0), threeCareers().get(1));
		assertThat(promptDue(st.session())).isFalse();
	}

	@Test
	void postReturns409WithFewerThanThreeReflections() {
		Student st = student();
		reflectOn(st.session(), threeCareers().get(0), threeCareers().get(1));
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isEqualTo(409);
		assertThat(intakeRows(st.id())).isZero();
	}

	@Test
	void promptDueAfterThreeReflectionsAndClearedBySubmit() {
		Student st = student();
		reflectThree(st.session());
		assertThat(promptDue(st.session())).isTrue();
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isCreated();
		assertThat(promptDue(st.session())).isFalse();
	}

	@Test
	void dismissClearsPromptButIntakeCanStillBeSubmitted() {
		Student st = student();
		reflectThree(st.session());
		dismiss(st.session()).expectStatus().isNoContent();
		assertThat(promptDue(st.session())).isFalse();
		dismiss(st.session()).expectStatus().isNoContent();
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isCreated();
	}

	@Test
	void twoInterestTagsSeedPlusHalfEach() {
		Student st = student();
		reflectThree(st.session());
		double healthBefore = score(st.id(), "Fixture health");
		double techBefore = score(st.id(), "Fixture technology");
		post(st.session(), interests(tagId("Fixture health"), tagId("Fixture technology"))).expectStatus()
				.isCreated();
		assertThat(score(st.id(), "Fixture health") - healthBefore).isEqualTo(0.5);
		assertThat(score(st.id(), "Fixture technology") - techBefore).isEqualTo(0.5);
	}

	@Test
	void secondPostIs409() {
		Student st = student();
		reflectThree(st.session());
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isCreated();
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isEqualTo(409);
	}

	@Test
	void putReplacesInterestSeedingAndAnswers() {
		Student st = student();
		reflectThree(st.session());
		long health = tagId("Fixture health");
		long tech = tagId("Fixture technology");
		double healthBefore = score(st.id(), "Fixture health");
		double techBefore = score(st.id(), "Fixture technology");
		post(st.session(), Map.of("answers", Map.of("interests", Map.of("tagIds", List.of(health)), "goals",
				Map.of("text", "be a nurse")))).expectStatus().isCreated();
		put(st.session(), Map.of("answers", Map.of("interests", Map.of("tagIds", List.of(tech)), "goals",
				Map.of("text", "be an engineer")))).expectStatus().isOk();
		assertThat(score(st.id(), "Fixture health")).isEqualTo(healthBefore);
		assertThat(score(st.id(), "Fixture technology") - techBefore).isEqualTo(0.5);
		String body = http.get().uri("/api/intake").headers(st.session()::apply).exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseBody();
		assertThat(body).contains("be an engineer").doesNotContain("be a nurse");
		assertThat(intakeRows(st.id())).isEqualTo(1);
	}

	@Test
	void putWithoutExistingIntakeIs404() {
		Student st = student();
		reflectThree(st.session());
		put(st.session(), interests(tagId("Fixture health"))).expectStatus().isNotFound();
	}

	@Test
	void validationRejectsUnknownKeyUnknownTagAndEmpty() {
		Student st = student();
		reflectThree(st.session());
		post(st.session(), Map.of("answers", Map.of("nope", Map.of("text", "x")))).expectStatus().isBadRequest();
		post(st.session(), interests(999999999L)).expectStatus().isBadRequest();
		post(st.session(), Map.of("answers", Map.of("goals", Map.of("text", "  ")))).expectStatus().isBadRequest();
		post(st.session(), Map.of("answers", Map.of("goals", Map.of("tagIds", List.of(1))))).expectStatus()
				.isBadRequest();
		assertThat(intakeRows(st.id())).isZero();
	}

	@Test
	void forcedFailureInSeedingLeavesNoIntakeSaved() {
		Student st = student();
		reflectThree(st.session());
		failure.fail.set(true);
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().is5xxServerError();
		assertThat(intakeRows(st.id())).isZero();
		assertThat(jdbc.queryForObject("select count(*) from intake_answers a join intake_responses r "
				+ "on r.id = a.response_id where r.student_id = ?", Long.class, st.id())).isZero();
		failure.fail.set(false);
		assertThat(promptDue(st.session())).isTrue();
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isCreated();
	}

	@Test
	void forcedFailureOnEditKeepsPreviousAnswersAndSeeding() {
		Student st = student();
		reflectThree(st.session());
		post(st.session(), interests(tagId("Fixture health"))).expectStatus().isCreated();
		double before = score(st.id(), "Fixture health");
		failure.fail.set(true);
		put(st.session(), interests(tagId("Fixture technology"))).expectStatus().is5xxServerError();
		assertThat(score(st.id(), "Fixture health")).isEqualTo(before);
	}

	@Test
	void studentOnlyAndAuthenticated() {
		http.post().uri("/api/intake/dismiss").headers(csrf()::apply).exchange().expectStatus().isUnauthorized();
	}

}
