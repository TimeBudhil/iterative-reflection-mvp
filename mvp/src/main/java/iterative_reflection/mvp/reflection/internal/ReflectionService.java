package iterative_reflection.mvp.reflection.internal;

import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.reflection.ReflectionSaved;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.AnswerInput;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.AnswerView;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.QuestionView;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.ReflectionView;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.SubmitRequest;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.SubmitResponse;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.Verdict;
import iterative_reflection.mvp.reflection.internal.ReflectionDtos.VersionView;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
class ReflectionService {

	private final JdbcClient jdbc;

	private final ReflectionRepository reflections;

	private final CareerCatalog careers;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ReflectionService(JdbcClient jdbc, ReflectionRepository reflections, CareerCatalog careers,
			ApplicationEventPublisher events, Clock clock) {
		this.jdbc = jdbc;
		this.reflections = reflections;
		this.careers = careers;
		this.events = events;
		this.clock = clock;
	}

	List<QuestionView> activeQuestions() {
		return jdbc.sql("""
				select id, question_key, version, prompt, open_prompt from reflection_questions
				where active = true order by display_order, id
				""").query((rs, n) -> new QuestionView(rs.getLong("id"), rs.getString("question_key"),
				rs.getInt("version"), rs.getString("prompt"), rs.getString("open_prompt"), 1, 10)).list();
	}

	@Transactional
	SubmitResponse submit(long studentId, long careerId, SubmitRequest request) {
		CareerSummary career = careers.findCareer(careerId).filter(CareerSummary::active)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		List<QuestionView> questions = activeQuestions();
		validateCoverage(questions, request.answers());
		if (careers.lastViewedPage(studentId, careerId) < career.lastPage()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "View the whole experience before reflecting.");
		}
		if (reflections.existsByStudentIdAndCareerId(studentId, careerId)) {
			throw alreadyReflected();
		}
		Instant now = clock.instant();
		ReflectionEntity reflection;
		try {
			reflection = reflections.saveAndFlush(new ReflectionEntity(studentId, careerId, now));
		}
		catch (DataIntegrityViolationException e) {
			throw alreadyReflected(); // lost a race with a concurrent submit: the unique constraint decides
		}
		double mean = writeVersion(reflection.id, 1, questions, request, now);
		events.publishEvent(new ReflectionSaved(studentId, careerId, mean, reflection.id));
		return new SubmitResponse(reflection.id, careerId, request.verdict(), 1);
	}

	/** A new version, answered against the currently active questions; replaces the affinity contribution. */
	@Transactional
	SubmitResponse edit(long studentId, long careerId, SubmitRequest request) {
		ReflectionEntity reflection = reflections.findByStudentIdAndCareerId(studentId, careerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		List<QuestionView> questions = activeQuestions();
		validateCoverage(questions, request.answers());
		Instant now = clock.instant();
		int next = reflection.currentVersion + 1;
		try {
			double mean = writeVersion(reflection.id, next, questions, request, now);
			reflection.currentVersion = next;
			reflection.updatedAt = now;
			reflections.saveAndFlush(reflection);
			events.publishEvent(new ReflectionSaved(studentId, careerId, mean, reflection.id));
		}
		catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "The reflection was changed concurrently; retry.");
		}
		return new SubmitResponse(reflection.id, careerId, request.verdict(), next);
	}

	Page<VersionView> versions(long studentId, long careerId, Pageable pageable) {
		long reflectionId = reflections.findByStudentIdAndCareerId(studentId, careerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).id;
		long total = jdbc.sql("select count(*) from reflection_versions where reflection_id = :r")
				.param("r", reflectionId).query(Long.class).single();
		record Header(long id, int number, Verdict verdict, Instant createdAt) {
		}
		List<Header> headers = jdbc.sql("""
				select id, version_number, verdict, created_at from reflection_versions
				where reflection_id = :r order by version_number desc limit :limit offset :offset
				""").param("r", reflectionId).param("limit", pageable.getPageSize())
				.param("offset", pageable.getOffset())
				.query((rs, n) -> new Header(rs.getLong("id"), rs.getInt("version_number"),
						Verdict.valueOf(rs.getString("verdict")), rs.getTimestamp("created_at").toInstant()))
				.list();
		Map<Long, List<AnswerView>> answers = answersFor(headers.stream().map(Header::id).toList());
		return new PageImpl<>(headers.stream()
				.map(h -> new VersionView(h.number(), h.verdict(), answers.get(h.id()), h.createdAt())).toList(),
				pageable, total);
	}

	/** Inserts a version with its answers and returns the mean score. */
	private double writeVersion(long reflectionId, int versionNumber, List<QuestionView> questions,
			SubmitRequest request, Instant now) {
		var keys = new GeneratedKeyHolder();
		jdbc.sql("""
				insert into reflection_versions (reflection_id, version_number, verdict, created_at)
				values (:r, :n, :v, :t)
				""").param("r", reflectionId).param("n", versionNumber).param("v", request.verdict().name())
				.param("t", Timestamp.from(now)).update(keys, "id");
		long versionId = keys.getKey().longValue();
		double sum = 0;
		for (QuestionView q : questions) {
			AnswerInput a = request.answers().get(q.key());
			jdbc.sql("""
					insert into reflection_answers (reflection_version_id, question_id, score, open_text)
					values (:v, :q, :s, :o)
					""").param("v", versionId).param("q", q.id()).param("s", a.score())
					.param("o", blankToNull(a.openText())).update();
			sum += a.score();
		}
		return sum / questions.size();
	}

	Page<ReflectionView> list(long studentId, Pageable pageable) {
		long total = jdbc.sql("select count(*) from reflections where student_id = :s").param("s", studentId)
				.query(Long.class).single();
		record Header(long id, long careerId, int versionNumber, Verdict verdict, Instant createdAt, Instant updatedAt,
				long versionId) {
		}
		List<Header> headers = jdbc.sql("""
				select r.id, r.career_id, r.current_version, r.created_at, r.updated_at, v.verdict, v.id as version_id
				from reflections r join reflection_versions v
				  on v.reflection_id = r.id and v.version_number = r.current_version
				where r.student_id = :s order by r.updated_at desc, r.id desc
				limit :limit offset :offset
				""").param("s", studentId).param("limit", pageable.getPageSize()).param("offset", pageable.getOffset())
				.query((rs, n) -> new Header(rs.getLong("id"), rs.getLong("career_id"), rs.getInt("current_version"),
						Verdict.valueOf(rs.getString("verdict")), rs.getTimestamp("created_at").toInstant(),
						rs.getTimestamp("updated_at").toInstant(), rs.getLong("version_id")))
				.list();
		Map<Long, List<AnswerView>> answers = answersFor(headers.stream().map(Header::versionId).toList());
		List<ReflectionView> content = headers.stream()
				.map(h -> new ReflectionView(h.id(), h.careerId(),
						careers.findCareer(h.careerId()).map(CareerSummary::title).orElse(""), h.verdict(),
						h.versionNumber(), answers.get(h.versionId()), h.createdAt(), h.updatedAt()))
				.toList();
		return new PageImpl<>(content, pageable, total);
	}

	private Map<Long, List<AnswerView>> answersFor(List<Long> versionIds) {
		Map<Long, List<AnswerView>> result = new LinkedHashMap<>();
		versionIds.forEach(id -> result.put(id, new ArrayList<>()));
		if (versionIds.isEmpty()) {
			return result;
		}
		jdbc.sql("""
				select a.reflection_version_id, q.question_key, q.version, q.prompt, a.score, a.open_text
				from reflection_answers a join reflection_questions q on q.id = a.question_id
				where a.reflection_version_id in (:ids) order by q.display_order, q.id
				""").param("ids", versionIds).query((rs, n) -> {
					result.get(rs.getLong("reflection_version_id")).add(new AnswerView(rs.getString("question_key"),
							rs.getInt("version"), rs.getString("prompt"), rs.getInt("score"), rs.getString("open_text")));
					return 0;
				}).list();
		return result;
	}

	private static String blankToNull(String s) {
		return s == null || s.isBlank() ? null : s;
	}

	private static ErrorResponseException alreadyReflected() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"You have already reflected on this career.");
		return new ErrorResponseException(HttpStatus.CONFLICT, problem, null);
	}

	/** Every active question needs a scale answer; unknown keys are rejected. Same shape as bean-validation errors. */
	private static void validateCoverage(List<QuestionView> questions, Map<String, AnswerInput> answers) {
		List<Map<String, String>> errors = new ArrayList<>();
		for (QuestionView q : questions) {
			if (!answers.containsKey(q.key())) {
				errors.add(Map.of("field", "answers[" + q.key() + "].score", "message",
						"A score from 1 to 10 is required."));
			}
		}
		for (String key : answers.keySet()) {
			if (questions.stream().noneMatch(q -> q.key().equals(key))) {
				errors.add(Map.of("field", "answers[" + key + "]", "message", "Unknown question."));
			}
		}
		if (!errors.isEmpty()) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed.");
			problem.setProperty("errors", errors);
			throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
		}
	}

}
