package iterative_reflection.mvp.reflection.internal.repository;

import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.ActivityRow;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.AnswerRow;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.CohortStats;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.QuestionRow;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.ReflectionHeader;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.VersionHeader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Hand-written SQL over the reflection tables (questions, versions, answers and the aggregate reads). */
@Repository
public class ReflectionJdbcRepository {

	private final JdbcClient jdbc;

	ReflectionJdbcRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<QuestionRow> activeQuestions() {
		return jdbc.sql("""
				select id, question_key, version, prompt, open_prompt from reflection_questions
				where active = true order by display_order, id
				""").query((rs, n) -> new QuestionRow(rs.getLong("id"), rs.getString("question_key"),
				rs.getInt("version"), rs.getString("prompt"), rs.getString("open_prompt"))).list();
	}

	public long countVersions(long reflectionId) {
		return jdbc.sql("select count(*) from reflection_versions where reflection_id = :r").param("r", reflectionId)
				.query(Long.class).single();
	}

	/** Newest version first. */
	public List<VersionHeader> versionHeaders(long reflectionId, int limit, long offset) {
		return jdbc.sql("""
				select id, version_number, verdict, created_at from reflection_versions
				where reflection_id = :r order by version_number desc limit :limit offset :offset
				""").param("r", reflectionId).param("limit", limit).param("offset", offset)
				.query((rs, n) -> new VersionHeader(rs.getLong("id"), rs.getInt("version_number"),
						rs.getString("verdict"), rs.getTimestamp("created_at").toInstant()))
				.list();
	}

	/** @return the new version's id */
	public long insertVersion(long reflectionId, int versionNumber, String verdict, Instant createdAt) {
		var keys = new GeneratedKeyHolder();
		jdbc.sql("""
				insert into reflection_versions (reflection_id, version_number, verdict, created_at)
				values (:r, :n, :v, :t)
				""").param("r", reflectionId).param("n", versionNumber).param("v", verdict)
				.param("t", Timestamp.from(createdAt)).update(keys, "id");
		return keys.getKey().longValue();
	}

	public void insertAnswer(long versionId, long questionId, int score, String openText) {
		jdbc.sql("""
				insert into reflection_answers (reflection_version_id, question_id, score, open_text)
				values (:v, :q, :s, :o)
				""").param("v", versionId).param("q", questionId).param("s", score).param("o", openText).update();
	}

	public int countByStudent(long studentId) {
		return jdbc.sql("select count(*) from reflections where student_id = :s").param("s", studentId)
				.query(Integer.class).single();
	}

	/** Current-version headers, most recently updated first. */
	public List<ReflectionHeader> currentHeaders(long studentId, int limit, long offset) {
		return jdbc.sql("""
				select r.id, r.career_id, r.current_version, r.created_at, r.updated_at, v.verdict, v.id as version_id
				from reflections r join reflection_versions v
				  on v.reflection_id = r.id and v.version_number = r.current_version
				where r.student_id = :s order by r.updated_at desc, r.id desc
				limit :limit offset :offset
				""").param("s", studentId).param("limit", limit).param("offset", offset)
				.query((rs, n) -> new ReflectionHeader(rs.getLong("id"), rs.getLong("career_id"),
						rs.getInt("current_version"), rs.getString("verdict"), rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant(), rs.getLong("version_id")))
				.list();
	}

	/** Answers per version id, in question display order; every requested id is present in the result. */
	public Map<Long, List<AnswerRow>> answersFor(Collection<Long> versionIds) {
		Map<Long, List<AnswerRow>> result = new LinkedHashMap<>();
		versionIds.forEach(id -> result.put(id, new ArrayList<>()));
		if (versionIds.isEmpty()) {
			return result;
		}
		jdbc.sql("""
				select a.reflection_version_id, q.question_key, q.version, q.prompt, a.score, a.open_text
				from reflection_answers a join reflection_questions q on q.id = a.question_id
				where a.reflection_version_id in (:ids) order by q.display_order, q.id
				""").param("ids", versionIds).query((rs, n) -> {
					long versionId = rs.getLong("reflection_version_id");
					result.get(versionId).add(new AnswerRow(versionId, rs.getString("question_key"),
							rs.getInt("version"), rs.getString("prompt"), rs.getInt("score"), rs.getString("open_text")));
					return 0;
				}).list();
		return result;
	}

	public Set<Long> reflectedCareerIds(long studentId) {
		return new HashSet<>(jdbc.sql("select career_id from reflections where student_id = :s")
				.param("s", studentId).query(Long.class).list());
	}

	/** Aggregates over the given cohort of student ids (empty cohort = all zeros). */
	public CohortStats cohortStats(int minReflections, Collection<Long> studentIds) {
		if (studentIds.isEmpty()) {
			return new CohortStats(0, 0, 0, 0);
		}
		long withMin = jdbc.sql("""
				select count(*) from (
				  select student_id from reflections where student_id in (:ids)
				  group by student_id having count(*) >= :min) x
				""").param("ids", studentIds).param("min", minReflections).query(Long.class).single();
		Map<String, Long> verdicts = new HashMap<>();
		jdbc.sql("""
				select v.verdict, count(*) as n
				from reflections r
				  join reflection_versions v on v.reflection_id = r.id and v.version_number = r.current_version
				where r.student_id in (:ids)
				group by v.verdict
				""").param("ids", studentIds).query((rs, i) -> {
					verdicts.put(rs.getString("verdict"), rs.getLong("n"));
					return 0;
				}).list();
		return new CohortStats(withMin, verdicts.getOrDefault("YES", 0L), verdicts.getOrDefault("MAYBE", 0L),
				verdicts.getOrDefault("NO", 0L));
	}

	public List<ActivityRow> activityOf(Collection<Long> studentIds) {
		if (studentIds.isEmpty()) {
			return List.of();
		}
		return jdbc.sql("""
				select r.student_id, count(*) as total,
				  sum(case when v.verdict = 'YES' then 1 else 0 end) as yes,
				  sum(case when v.verdict = 'MAYBE' then 1 else 0 end) as maybe,
				  sum(case when v.verdict = 'NO' then 1 else 0 end) as no,
				  max(r.updated_at) as last_activity
				from reflections r join reflection_versions v
				  on v.reflection_id = r.id and v.version_number = r.current_version
				where r.student_id in (:ids) group by r.student_id
				""").param("ids", studentIds)
				.query((rs, n) -> new ActivityRow(rs.getLong("student_id"), rs.getInt("total"), rs.getInt("yes"),
						rs.getInt("maybe"), rs.getInt("no"), rs.getTimestamp("last_activity").toInstant()))
				.list();
	}

}
