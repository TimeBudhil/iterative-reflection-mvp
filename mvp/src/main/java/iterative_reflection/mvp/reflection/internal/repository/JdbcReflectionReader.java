package iterative_reflection.mvp.reflection.internal.repository;

import iterative_reflection.mvp.reflection.internal.services.ReflectionService;
import iterative_reflection.mvp.reflection.PilotReflectionStats;
import iterative_reflection.mvp.reflection.ReflectionActivity;
import iterative_reflection.mvp.reflection.ReflectionReader;
import iterative_reflection.mvp.reflection.ReflectionRecord;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class JdbcReflectionReader implements ReflectionReader {

	private final JdbcClient jdbc;

	private final ReflectionService reflections;

	JdbcReflectionReader(JdbcClient jdbc, ReflectionService reflections) {
		this.jdbc = jdbc;
		this.reflections = reflections;
	}

	@Override
	public int reflectionCount(long studentId) {
		return jdbc.sql("select count(*) from reflections where student_id = :s").param("s", studentId)
				.query(Integer.class).single();
	}

	@Override
	public Set<Long> reflectedCareerIds(long studentId) {
		return new HashSet<>(jdbc.sql("select career_id from reflections where student_id = :s")
				.param("s", studentId).query(Long.class).list());
	}

	@Override
	public List<ReflectionRecord> reflectionsOf(long studentId) {
		return reflections.list(studentId, PageRequest.of(0, Integer.MAX_VALUE)).getContent().stream()
				.map(r -> new ReflectionRecord(r.id(), r.careerId(), r.careerTitle(), r.verdict().name(),
						r.versionNumber(),
						r.answers().stream().map(a -> new ReflectionRecord.Answer(a.key(), a.questionVersion(),
								a.prompt(), a.score(), a.openText())).toList(),
						r.createdAt(), r.updatedAt()))
				.toList();
	}

	@Override
	public PilotReflectionStats pilotStats(int minReflections, java.time.Instant from, java.time.Instant to) {
		java.sql.Timestamp f = from == null ? null : java.sql.Timestamp.from(from);
		java.sql.Timestamp t = to == null ? null : java.sql.Timestamp.from(to);
		String cohort = """
				 u.role = 'STUDENT' and u.active = true and u.demo = false
				  and (cast(:from as timestamp) is null or u.created_at >= :from)
				  and (cast(:to as timestamp) is null or u.created_at < :to)
				""";
		long withMin = jdbc.sql("""
				select count(*) from (
				  select r.student_id from reflections r join users u on u.id = r.student_id
				  where""" + cohort + """
				  group by r.student_id having count(*) >= :min) x
				""").param("from", f).param("to", t).param("min", minReflections).query(Long.class).single();
		Map<String, Long> verdicts = new HashMap<>();
		jdbc.sql("""
				select v.verdict, count(*) as n
				from reflections r join users u on u.id = r.student_id
				  join reflection_versions v on v.reflection_id = r.id and v.version_number = r.current_version
				where""" + cohort + """
				group by v.verdict
				""").param("from", f).param("to", t).query((rs, i) -> {
					verdicts.put(rs.getString("verdict"), rs.getLong("n"));
					return 0;
				}).list();
		return new PilotReflectionStats(withMin, verdicts.getOrDefault("YES", 0L), verdicts.getOrDefault("MAYBE", 0L),
				verdicts.getOrDefault("NO", 0L));
	}

	@Override
	public Map<Long, ReflectionActivity> activityOf(Collection<Long> studentIds) {
		Map<Long, ReflectionActivity> result = new HashMap<>();
		if (studentIds.isEmpty()) {
			return result;
		}
		jdbc.sql("""
				select r.student_id, count(*) as total,
				  sum(case when v.verdict = 'YES' then 1 else 0 end) as yes,
				  sum(case when v.verdict = 'MAYBE' then 1 else 0 end) as maybe,
				  sum(case when v.verdict = 'NO' then 1 else 0 end) as no,
				  max(r.updated_at) as last_activity
				from reflections r join reflection_versions v
				  on v.reflection_id = r.id and v.version_number = r.current_version
				where r.student_id in (:ids) group by r.student_id
				""").param("ids", studentIds).query((rs, n) -> {
					result.put(rs.getLong("student_id"), new ReflectionActivity(rs.getInt("total"), rs.getInt("yes"),
							rs.getInt("maybe"), rs.getInt("no"), rs.getTimestamp("last_activity").toInstant()));
					return 0;
				}).list();
		return result;
	}

}
