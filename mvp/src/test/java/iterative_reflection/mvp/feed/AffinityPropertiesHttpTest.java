package iterative_reflection.mvp.feed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** Overridden affinity constants are honoured [R6.6]: (10 - 4) / 3 = 2.0. */
@TestPropertySource(properties = { "affinity.neutral-score=4", "affinity.scale=3" })
class AffinityPropertiesHttpTest extends FeedTestSupport {

	@Autowired
	FeedService feed;

	@Test
	void overriddenNeutralAndScaleChangeTheDelta() {
		String email = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		Session s = signIn(email, "correct horse battery");
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email.toLowerCase());
		reflect(s, careerId("Fixture Career Beta"), 10);
		assertThat(feed.topAffinities(id, 5)).singleElement().extracting(TagAffinity::score).isEqualTo(2.0);
	}

}
