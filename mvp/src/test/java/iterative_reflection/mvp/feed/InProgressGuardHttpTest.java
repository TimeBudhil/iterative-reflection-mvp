package iterative_reflection.mvp.feed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** R4.2 and the abandon rules hold even when no card is stored (never served, or after abandoning) [R4.2, R4.7]. */
class InProgressGuardHttpTest extends FeedTestSupport {

	ResponseSpec abandon(Session s, long career) {
		return http.post().uri("/api/careers/{id}/abandon", career).headers(s::apply).exchange();
	}

	ResponseSpec experience(Session s, long career) {
		return http.get().uri("/api/careers/{id}/experience", career).headers(s::apply).exchange();
	}

	ResponseSpec reflectionPost(Session s, long career) {
		return http.post().uri("/api/careers/{id}/reflection", career).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("answers", Map.of(), "verdict", "YES")).exchange();
	}

	@Test
	void doubleTappedAbandonIsRejectedTheSecondTime() {
		Session s = signedInStudent();
		long served = nextId(s);
		viewPage(s, served, 1).expectStatus().isOk();
		abandon(s, served).expectStatus().isNoContent();
		abandon(s, served).expectStatus().isEqualTo(409);
	}

	@Test
	void careerNeverServedCannotBeOpenedViewedAbandonedOrReflectedOn() {
		Session s = signedInStudent();
		long other = careerId("Fixture Career Alpha");
		serve(s, careerId("Fixture Career Beta"));
		experience(s, other).expectStatus().isEqualTo(409);
		viewPage(s, other, 1).expectStatus().isEqualTo(409);
		abandon(s, other).expectStatus().isEqualTo(409);
		reflectionPost(s, other).expectStatus().isEqualTo(409);
	}

	@Test
	void noCardStoredMeansNoCareerIsOpenable() {
		Session s = signedInStudent();
		long career = careerId("Fixture Career Alpha");
		experience(s, career).expectStatus().isEqualTo(409);
		viewPage(s, career, 1).expectStatus().isEqualTo(409);
		reflectionPost(s, career).expectStatus().isEqualTo(409);
	}

	@Test
	void reflectedCareerStaysOpenableForReview() {
		Session s = signedInStudent();
		long career = careerId("Fixture Career Alpha");
		serve(s, career);
		reflect(s, career, 7);
		experience(s, career).expectStatus().isOk();
		http.get().uri("/api/careers/{id}/reflection/versions", career).headers(s::apply).exchange().expectStatus()
				.isOk();
		assertThat(nextId(s)).isNotEqualTo(career);
	}

}
