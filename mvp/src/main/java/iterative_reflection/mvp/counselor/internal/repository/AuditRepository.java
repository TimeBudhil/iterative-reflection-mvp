package iterative_reflection.mvp.counselor.internal.repository;

import iterative_reflection.mvp.counselor.AuditEntry;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL over the append-only {@code audit_log}. */
@Repository
public class AuditRepository {

	private final JdbcClient jdbc;

	AuditRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insert(long counselorId, long studentId, String outcome, Instant at) {
		jdbc.sql("insert into audit_log (counselor_id, student_id, outcome, at) values (:c, :s, :o, :t)")
				.param("c", counselorId).param("s", studentId).param("o", outcome).param("t", Timestamp.from(at))
				.update();
	}

	/** Newest first; all students when {@code studentId} is null. */
	public List<AuditEntry> find(Long studentId, int limit, long offset) {
		String where = studentId == null ? "" : " where student_id = :s";
		var rows = jdbc.sql("select id, counselor_id, student_id, outcome, at from audit_log" + where
				+ " order by at desc, id desc limit :limit offset :offset").param("limit", limit)
				.param("offset", offset);
		if (studentId != null) {
			rows = rows.param("s", studentId);
		}
		return rows.query((rs, n) -> new AuditEntry(rs.getLong("id"), rs.getLong("counselor_id"),
				rs.getLong("student_id"), rs.getString("outcome"), rs.getTimestamp("at").toInstant())).list();
	}

	public long count(Long studentId) {
		var count = jdbc.sql("select count(*) from audit_log" + (studentId == null ? "" : " where student_id = :s"));
		if (studentId != null) {
			count = count.param("s", studentId);
		}
		return count.query(Long.class).single();
	}

}
