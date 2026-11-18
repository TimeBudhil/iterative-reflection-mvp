package iterative_reflection.mvp.reflection.internal.services;

import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.ReflectionHeader;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionRows.VersionHeader;
import iterative_reflection.mvp.reflection.internal.repository.ReflectionJdbcRepository;
import iterative_reflection.mvp.reflection.internal.repository.ReflectionRepository;
import iterative_reflection.mvp.reflection.internal.domain.ReflectionEntity;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.reflection.ReflectionSaved;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.AnswerInput;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.AnswerView;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.QuestionView;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.ReflectionView;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.SubmitRequest;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.SubmitResponse;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.Verdict;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.VersionView;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReflectionService {

	private final ReflectionJdbcRepository queries;

	private final ReflectionRepository reflections;

	private final CareerCatalog careers;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ReflectionService(ReflectionJdbcRepository queries, ReflectionRepository reflections, CareerCatalog careers,
			ApplicationEventPublisher events, Clock clock) {
		this.queries = queries;
		this.reflections = reflections;
		this.careers = careers;
		this.events = events;
		this.clock = clock;
	}

	public List<QuestionView> activeQuestions() {
		return queries.activeQuestions().stream().map(q -> new QuestionView(q.id(), q.key(), q.version(), q.prompt(),
				q.openPrompt(), 1, 10)).toList();
	}

	@Transactional
	public SubmitResponse submit(long studentId, long careerId, SubmitRequest request) {
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
	public SubmitResponse edit(long studentId, long careerId, SubmitRequest request) {
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

	public Page<VersionView> versions(long studentId, long careerId, Pageable pageable) {
		long reflectionId = reflections.findByStudentIdAndCareerId(studentId, careerId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).id;
		long total = queries.countVersions(reflectionId);
		List<VersionHeader> headers = queries.versionHeaders(reflectionId, pageable.getPageSize(),
				pageable.getOffset());
		Map<Long, List<AnswerView>> answers = answersFor(headers.stream().map(VersionHeader::id).toList());
		return new PageImpl<>(headers.stream().map(h -> new VersionView(h.number(), Verdict.valueOf(h.verdict()),
				answers.get(h.id()), h.createdAt())).toList(), pageable, total);
	}

	/** Inserts a version with its answers and returns the mean score. */
	private double writeVersion(long reflectionId, int versionNumber, List<QuestionView> questions,
			SubmitRequest request, Instant now) {
		long versionId = queries.insertVersion(reflectionId, versionNumber, request.verdict().name(), now);
		double sum = 0;
		for (QuestionView q : questions) {
			AnswerInput a = request.answers().get(q.key());
			queries.insertAnswer(versionId, q.id(), a.score(), blankToNull(a.openText()));
			sum += a.score();
		}
		return sum / questions.size();
	}

	public Page<ReflectionView> list(long studentId, Pageable pageable) {
		long total = queries.countByStudent(studentId);
		List<ReflectionHeader> headers = queries.currentHeaders(studentId, pageable.getPageSize(),
				pageable.getOffset());
		Map<Long, List<AnswerView>> answers = answersFor(headers.stream().map(ReflectionHeader::versionId).toList());
		List<ReflectionView> content = headers.stream()
				.map(h -> new ReflectionView(h.id(), h.careerId(),
						careers.findCareer(h.careerId()).map(CareerSummary::title).orElse(""),
						Verdict.valueOf(h.verdict()), h.versionNumber(), answers.get(h.versionId()), h.createdAt(),
						h.updatedAt()))
				.toList();
		return new PageImpl<>(content, pageable, total);
	}

	private Map<Long, List<AnswerView>> answersFor(List<Long> versionIds) {
		Map<Long, List<AnswerView>> result = new LinkedHashMap<>();
		queries.answersFor(versionIds).forEach((versionId, rows) -> result.put(versionId, rows.stream()
				.map(a -> new AnswerView(a.key(), a.questionVersion(), a.prompt(), a.score(), a.openText()))
				.toList()));
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
