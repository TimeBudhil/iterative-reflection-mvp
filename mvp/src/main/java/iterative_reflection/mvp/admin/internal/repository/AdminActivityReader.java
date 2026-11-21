package iterative_reflection.mvp.admin.internal.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads of {@code admin_activity}, newest first. */
@Repository
public class AdminActivityReader {

	public record Row(long id, long adminId, String action, String detailJson, Instant createdAt) {
	}

	private final JdbcClient jdbc;

	AdminActivityReader(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<Row> find(Pageable pageable) {
		return jdbc.sql("select id, admin_id, action, detail, created_at from admin_activity"
				+ " order by created_at desc, id desc limit :limit offset :offset")
				.param("limit", pageable.getPageSize()).param("offset", pageable.getOffset())
				.query((rs, n) -> new Row(rs.getLong("id"), rs.getLong("admin_id"), rs.getString("action"),
						rs.getString("detail"), rs.getTimestamp("created_at").toInstant()))
				.list();
	}

	public long count() {
		return jdbc.sql("select count(*) from admin_activity").query(Long.class).single();
	}

}
