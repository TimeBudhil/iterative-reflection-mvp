package iterative_reflection.mvp.admin.internal.repository;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts one {@code admin_activity} row in its own transaction. It throws on failure; {@code AdminActivity} is the
 * best-effort wrapper callers use, so tests can replace this bean with one that throws.
 */
@Repository
public class AdminActivityWriter {

	private final JdbcClient jdbc;

	AdminActivityWriter(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void insert(long adminId, String action, String detailJson, Instant at) {
		jdbc.sql("insert into admin_activity (admin_id, action, detail, created_at) values (:a, :x, :d, :t)")
				.param("a", adminId).param("x", action).param("d", detailJson).param("t", Timestamp.from(at))
				.update();
	}

}
