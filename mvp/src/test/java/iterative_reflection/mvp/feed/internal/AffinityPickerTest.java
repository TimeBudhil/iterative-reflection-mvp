package iterative_reflection.mvp.feed.internal;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.career.TagView;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * Picker behaviour on hand-built pools of fixture careers (the catalog may also hold seed careers, so the pool is
 * explicit). Exploration every 3rd card; random source is stubbed to always choose the last pool member; abandons are
 * stubbed so the slot counter can be moved without HTTP.
 */
@TestPropertySource(properties = "feed.exploration-every=3")
class AffinityPickerTest extends HttpIntegrationTest {

	static final AtomicLong ABANDONS = new AtomicLong();

	@TestConfiguration
	static class Stubs {

		@Bean
		@Primary
		AbandonCounter abandonCounter() {
			return studentId -> ABANDONS.get();
		}

		@Bean
		@Primary
		RandomGenerator lastIndexRandom() {
			return new RandomGenerator() {
				@Override
				public long nextLong() {
					return 0;
				}

				@Override
				public int nextInt(int bound) {
					return bound - 1;
				}
			};
		}

	}

	@Autowired
	AffinityPicker picker;

	@Autowired
	CareerCatalog catalog;

	CareerSummary alpha;

	CareerSummary beta;

	long student;

	@BeforeEach
	void setUp() {
		ABANDONS.set(0);
		alpha = career("Fixture Career Alpha");
		beta = career("Fixture Career Beta");
		String email = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		student = jdbc.queryForObject("select id from users where email = ?", Long.class, email.toLowerCase());
	}

	private CareerSummary career(String title) {
		long id = jdbc.queryForObject("select id from careers where title = ?", Long.class, title);
		return catalog.findCareer(id).orElseThrow();
	}

	private void contribute(CareerSummary c, double delta) {
		for (TagView t : c.tags()) {
			jdbc.update("insert into affinity_contributions (student_id, tag_id, source_type, source_id, delta) "
					+ "values (?, ?, 'REFLECTION', ?, ?)", student, t.id(), c.id(), delta);
		}
	}

	@Test
	void coldStartKeepsPoolOrder() {
		assertThat(picker.pick(student, List.of(beta, alpha)).orElseThrow().id()).isEqualTo(beta.id());
	}

	@Test
	void highestSummedAffinityWinsOverPoolOrder() {
		contribute(alpha, 0.5);
		assertThat(picker.pick(student, List.of(beta, alpha)).orElseThrow().id()).isEqualTo(alpha.id());
	}

	@Test
	void sumsAcrossTags() {
		// Alpha has two tags (+0.4 each = 0.8); Beta one tag at +0.5
		contribute(alpha, 0.4);
		contribute(beta, 0.5);
		assertThat(picker.pick(student, List.of(beta, alpha)).orElseThrow().id()).isEqualTo(alpha.id());
	}

	@Test
	void negativeAffinityRanksBelowNeutral() {
		contribute(beta, -0.5);
		assertThat(picker.pick(student, List.of(beta, alpha)).orElseThrow().id()).isEqualTo(alpha.id());
	}

	@Test
	void everyThirdPickIsTheRandomOneEvenOnColdStart() {
		contribute(alpha, 0.5);
		List<CareerSummary> pool = List.of(alpha, beta);
		long[] picked = new long[3];
		for (int i = 0; i < 3; i++) {
			ABANDONS.set(i); // slot counter = reflections (0) + abandons
			picked[i] = picker.pick(student, pool).orElseThrow().id();
		}
		assertThat(picked).containsExactly(alpha.id(), alpha.id(), beta.id());
	}

	@Test
	void emptyPoolPicksNothing() {
		assertThat(picker.pick(student, List.of())).isEmpty();
	}

}
