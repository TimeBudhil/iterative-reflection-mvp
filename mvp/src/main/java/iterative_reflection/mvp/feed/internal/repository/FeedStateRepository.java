package iterative_reflection.mvp.feed.internal.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** The one stored feed card per student ({@code feed_state}). */
@Repository
public class FeedStateRepository {

	private final JdbcClient jdbc;

	FeedStateRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public Optional<Long> storedCareerId(long studentId) {
		return jdbc.sql("select career_id from feed_state where student_id = :s").param("s", studentId)
				.query(Long.class).optional();
	}

	/** @throws DuplicateKeyException when the student already has a card (a lost race) */
	public void insert(long studentId, long careerId, Instant servedAt) throws DuplicateKeyException {
		jdbc.sql("insert into feed_state (student_id, career_id, served_at) values (:s, :c, :t)")
				.param("s", studentId).param("c", careerId).param("t", Timestamp.from(servedAt)).update();
	}

	public void delete(long studentId, long careerId) {
		jdbc.sql("delete from feed_state where student_id = :s and career_id = :c").param("s", studentId)
				.param("c", careerId).update();
	}

}
