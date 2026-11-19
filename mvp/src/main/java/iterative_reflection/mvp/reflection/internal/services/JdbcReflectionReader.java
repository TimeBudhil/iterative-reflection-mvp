package iterative_reflection.mvp.reflection.internal.services;

import iterative_reflection.mvp.reflection.PilotReflectionStats;
import iterative_reflection.mvp.reflection.ReflectionActivity;
import iterative_reflection.mvp.reflection.ReflectionReader;
import iterative_reflection.mvp.reflection.ReflectionRecord;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.CohortStats;
import iterative_reflection.mvp.reflection.internal.repository.ReflectionJdbcRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class JdbcReflectionReader implements ReflectionReader {

	private final ReflectionJdbcRepository repository;

	private final ReflectionService reflections;

	JdbcReflectionReader(ReflectionJdbcRepository repository, ReflectionService reflections) {
		this.repository = repository;
		this.reflections = reflections;
	}

	@Override
	public int reflectionCount(long studentId) {
		return repository.countByStudent(studentId);
	}

	@Override
	public Set<Long> reflectedCareerIds(long studentId) {
		return repository.reflectedCareerIds(studentId);
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
	public PilotReflectionStats pilotStats(int minReflections, Collection<Long> cohortStudentIds) {
		CohortStats s = repository.cohortStats(minReflections, cohortStudentIds);
		return new PilotReflectionStats(s.studentsWithMin(), s.yes(), s.maybe(), s.no());
	}

	@Override
	public Map<Long, ReflectionActivity> activityOf(Collection<Long> studentIds) {
		Map<Long, ReflectionActivity> result = new HashMap<>();
		repository.activityOf(studentIds).forEach(a -> result.put(a.studentId(),
				new ReflectionActivity(a.total(), a.yes(), a.maybe(), a.no(), a.lastActivity())));
		return result;
	}

}
