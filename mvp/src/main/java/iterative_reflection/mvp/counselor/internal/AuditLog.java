package iterative_reflection.mvp.counselor.internal;

import iterative_reflection.mvp.counselor.AuditEntry;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Append-only record of counselor detail reads [R8.6]. */
@Service
class AuditLog {

	private final JdbcClient jdbc;

	private final Clock clock;

	AuditLog(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	/** Own transaction: a denied request fails with 403 but its row must survive any enclosing rollback. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void record(long counselorId, long studentId, boolean allowed) {
		jdbc.sql("insert into audit_log (counselor_id, student_id, outcome, at) values (:c, :s, :o, :t)")
				.param("c", counselorId).param("s", studentId).param("o", allowed ? "ALLOWED" : "DENIED")
				.param("t", Timestamp.from(clock.instant())).update();
	}

	/** Newest first; all students when {@code studentId} is null. */
	@Transactional(readOnly = true)
	Page<AuditEntry> find(Long studentId, Pageable pageable) {
		String where = studentId == null ? "" : " where student_id = :s";
		var count = jdbc.sql("select count(*) from audit_log" + where);
		var rows = jdbc.sql("select id, counselor_id, student_id, outcome, at from audit_log" + where
				+ " order by at desc, id desc limit :limit offset :offset").param("limit", pageable.getPageSize())
				.param("offset", pageable.getOffset());
		if (studentId != null) {
			count = count.param("s", studentId);
			rows = rows.param("s", studentId);
		}
		List<AuditEntry> content = rows.query((rs, n) -> new AuditEntry(rs.getLong("id"), rs.getLong("counselor_id"),
				rs.getLong("student_id"), rs.getString("outcome"), rs.getTimestamp("at").toInstant())).list();
		return new PageImpl<>(content, pageable, count.query(Long.class).single());
	}

}
