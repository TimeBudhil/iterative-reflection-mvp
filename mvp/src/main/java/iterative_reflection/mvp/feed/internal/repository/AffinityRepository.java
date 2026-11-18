package iterative_reflection.mvp.feed.internal.repository;

import iterative_reflection.mvp.feed.internal.domain.FeedRows.TagScore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Rows per (student, tag, source type, source id) in {@code affinity_contributions}; a tag's score is their sum. */
@Repository
public class AffinityRepository {

	private final JdbcClient jdbc;

	AffinityRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public void insert(long studentId, long tagId, String sourceType, long sourceId, double delta) {
		jdbc.sql("""
				insert into affinity_contributions (student_id, tag_id, source_type, source_id, delta)
				values (:s, :t, :type, :id, :d)""").param("s", studentId).param("t", tagId)
				.param("type", sourceType).param("id", sourceId).param("d", delta).update();
	}

	public void delete(long studentId, String sourceType, long sourceId) {
		jdbc.sql("delete from affinity_contributions where student_id = :s and source_type = :type and source_id = :id")
				.param("s", studentId).param("type", sourceType).param("id", sourceId).update();
	}

	/** Summed affinity per tag id (tags with no contributions are absent). */
	public Map<Long, Double> scores(long studentId) {
		Map<Long, Double> out = new HashMap<>();
		jdbc.sql("select tag_id, sum(delta) as score from affinity_contributions where student_id = :s group by tag_id")
				.param("s", studentId).query((rs, i) -> {
					out.put(rs.getLong("tag_id"), rs.getDouble("score"));
					return null;
				}).list();
		return out;
	}

	/** Highest summed score first (ties by tag id), at most {@code n} tags. */
	public List<TagScore> top(long studentId, int n) {
		return jdbc.sql("""
				select tag_id, sum(delta) as score from affinity_contributions where student_id = :s
				group by tag_id order by score desc, tag_id limit :n""").param("s", studentId)
				.param("n", Math.max(n, 0)).query((rs, i) -> new TagScore(rs.getLong("tag_id"), rs.getDouble("score")))
				.list();
	}

}
