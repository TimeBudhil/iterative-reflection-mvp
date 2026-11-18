package iterative_reflection.mvp.feed.internal;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Every abandon event counts toward the exploration slot, including the second abandon of one career. */
class AbandonCounterTest extends HttpIntegrationTest {

	@Autowired
	AbandonCounter counter;

	@Test
	void countsAbandonEventsNotRows() {
		String email = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		long student = jdbc.queryForObject("select id from users where email = ?", Long.class, email.toLowerCase());
		assertThat(counter.abandons(student)).isZero();
		jdbc.update("""
				insert into abandons (student_id, career_id, last_viewed_page, abandon_count, abandoned_at)
				select ?, id, 1, 2, current_timestamp from careers order by id limit 1""", student);
		jdbc.update("""
				insert into abandons (student_id, career_id, last_viewed_page, abandon_count, abandoned_at)
				select ?, id, 1, 1, current_timestamp from careers order by id limit 1 offset 1""", student);
		assertThat(counter.abandons(student)).isEqualTo(3);
	}

}
