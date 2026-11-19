package iterative_reflection.mvp.feed.internal.repository;

import iterative_reflection.mvp.feed.internal.domain.FeedRows.AbandonRow;
import iterative_reflection.mvp.feed.internal.domain.FeedRows.AbandonSummary;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** One row per student and career ({@code abandons}); {@code abandon_count} 2 = final. */
@Repository
public class AbandonRepository {

	private final JdbcClient jdbc;

	AbandonRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Career id to abandon count (1 or 2). */
	public Map<Long, Integer> counts(long studentId) {
		Map<Long, Integer> out = new HashMap<>();
		jdbc.sql("select career_id, abandon_count from abandons where student_id = :s").param("s", studentId)
				.query((rs, i) -> {
					out.put(rs.getLong("career_id"), rs.getInt("abandon_count"));
					return null;
				}).list();
		return out;
	}

	public void insertFirst(long studentId, long careerId, int lastViewedPage, Instant at) {
		jdbc.sql("""
				insert into abandons (student_id, career_id, last_viewed_page, abandon_count, abandoned_at)
				values (:s, :c, :p, 1, :t)""").param("s", studentId).param("c", careerId).param("p", lastViewedPage)
				.param("t", Timestamp.from(at)).update();
	}

	public void markFinal(long studentId, long careerId, int lastViewedPage, Instant at) {
		jdbc.sql("""
				update abandons set last_viewed_page = :p, abandon_count = 2, abandoned_at = :t
				where student_id = :s and career_id = :c""").param("s", studentId).param("c", careerId)
				.param("p", lastViewedPage).param("t", Timestamp.from(at)).update();
	}

	public long idOf(long studentId, long careerId) {
		return jdbc.sql("select id from abandons where student_id = :s and career_id = :c").param("s", studentId)
				.param("c", careerId).query(Long.class).single();
	}

	/** Newest first. */
	public List<AbandonRow> rows(long studentId) {
		return jdbc.sql("""
				select career_id, last_viewed_page, abandon_count, abandoned_at
				from abandons where student_id = :s order by abandoned_at desc, id desc""").param("s", studentId)
				.query((rs, i) -> new AbandonRow(rs.getLong("career_id"), rs.getInt("last_viewed_page"),
						rs.getInt("abandon_count"), rs.getTimestamp("abandoned_at").toInstant()))
				.list();
	}

	public long totalAbandons(long studentId) {
		return jdbc.sql("select coalesce(sum(abandon_count), 0) from abandons where student_id = :s")
				.param("s", studentId).query(Long.class).single();
	}

	/** Abandoned careers and latest abandon time per student, in one query; students with none are absent. */
	public List<AbandonSummary> summaryOf(Collection<Long> studentIds) {
		return jdbc.sql("""
				select student_id, count(*) as total, max(abandoned_at) as last_at
				from abandons where student_id in (:ids) group by student_id
				""").param("ids", studentIds)
				.query((rs, n) -> new AbandonSummary(rs.getLong("student_id"), rs.getInt("total"),
						rs.getTimestamp("last_at").toInstant()))
				.list();
	}

}
