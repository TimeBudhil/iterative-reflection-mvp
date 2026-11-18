package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.AffinitySource;
import iterative_reflection.mvp.feed.TagAffinity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place that writes {@code affinity_contributions}. Contributions are rows per (student, tag, source type,
 * source id); a tag's score is their sum. Replacing a source's contribution is "delete that source's rows, insert new
 * ones", so nothing is ever double counted. Writes require an enclosing transaction (the caller's listener runs inside
 * the reflection/intake/abandon transaction, so a failure rolls that back too) [R6.5].
 *
 * <p>
 * Tickets: 11 (reflection edit) calls {@link #replaceForCareer} again with the same source id, 12 uses source ABANDON
 * (source id = abandon id), 13 uses INTAKE with {@link #replace} (tags chosen in intake).
 */
@Component
public class AffinityApplier {

	private final JdbcClient jdbc;

	private final CareerTags careerTags;

	AffinityApplier(JdbcClient jdbc, CareerTags careerTags) {
		this.jdbc = jdbc;
		this.careerTags = careerTags;
	}

	/** Replaces the source's rows with {@code delta} on each tag of the career. */
	@Transactional(propagation = Propagation.MANDATORY)
	void replaceForCareer(long studentId, AffinitySource source, long sourceId, long careerId, double delta) {
		Map<Long, Double> perTag = new HashMap<>();
		careerTags.tagIds(careerId).forEach(t -> perTag.put(t, delta));
		replace(studentId, source, sourceId, perTag);
	}

	/** Replaces the source's rows with the given per-tag deltas (empty map = just remove). */
	@Transactional(propagation = Propagation.MANDATORY)
	void replace(long studentId, AffinitySource source, long sourceId, Map<Long, Double> deltaByTag) {
		remove(studentId, source, sourceId);
		deltaByTag.forEach((tag, delta) -> jdbc.sql("""
				insert into affinity_contributions (student_id, tag_id, source_type, source_id, delta)
				values (:s, :t, :type, :id, :d)""").param("s", studentId).param("t", tag)
				.param("type", source.name()).param("id", sourceId).param("d", delta).update());
	}

	@Transactional(propagation = Propagation.MANDATORY)
	void remove(long studentId, AffinitySource source, long sourceId) {
		jdbc.sql("delete from affinity_contributions where student_id = :s and source_type = :type and source_id = :id")
				.param("s", studentId).param("type", source.name()).param("id", sourceId).update();
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

	List<TagAffinity> top(long studentId, int n) {
		return jdbc.sql("""
				select t.id, t.name, c.name as category, sum(a.delta) as score
				from affinity_contributions a
				join tags t on t.id = a.tag_id
				join tag_categories c on c.id = t.category_id
				where a.student_id = :s
				group by t.id, t.name, c.name
				order by score desc, t.id
				limit :n""").param("s", studentId).param("n", Math.max(n, 0))
				.query((rs, i) -> new TagAffinity(rs.getLong("id"), rs.getString("name"), rs.getString("category"),
						rs.getDouble("score")))
				.list();
	}

}
