package iterative_reflection.mvp.intake.internal.services;

import iterative_reflection.mvp.intake.internal.dto.IntakeDtos;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.TagView;
import iterative_reflection.mvp.identity.IntakePromptProvider;
import iterative_reflection.mvp.intake.IntakeReader;
import iterative_reflection.mvp.intake.IntakeSaved;
import iterative_reflection.mvp.intake.IntakeView;
import iterative_reflection.mvp.intake.internal.domain.IntakeRows.Question;
import iterative_reflection.mvp.intake.internal.domain.IntakeRows.ResponseHead;
import iterative_reflection.mvp.intake.internal.repository.IntakeJdbcRepository;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

/**
 * Intake questions, the one response per student, dismissal and the prompt rule. Saves publish
 * {@link IntakeSaved} synchronously inside the transaction (feed seeds affinity from it), so a listener failure
 * rolls the whole intake back [N3.2].
 */
@Service
public class IntakeService implements IntakeReader, IntakePromptProvider {

	private static final int MAX_TEXT = 2000;

	private final IntakeJdbcRepository repository;

	private final CareerCatalog catalog;

	private final ReflectionReader reflections;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	private final int minReflections;

	IntakeService(IntakeJdbcRepository repository, CareerCatalog catalog, ReflectionReader reflections,
			ApplicationEventPublisher events, Clock clock, @Value("${intake.min-reflections:3}") int minReflections) {
		this.repository = repository;
		this.catalog = catalog;
		this.reflections = reflections;
		this.events = events;
		this.clock = clock;
		this.minReflections = minReflections;
	}

	public List<IntakeDtos.QuestionView> questions() {
		List<TagView> tags = catalog.allTags();
		return activeQuestions().stream().map(q -> new IntakeDtos.QuestionView(q.id(), q.key(), q.prompt(),
				q.type(), q.tags() ? tags : null)).toList();
	}

	@Transactional
	public IntakeView submit(long studentId, IntakeDtos.IntakeRequest request) {
		if (reflections.reflectionCount(studentId) < minReflections) {
			throw problem(HttpStatus.CONFLICT,
					"Reflect on at least " + minReflections + " careers before filling in the intake.");
		}
		Map<Question, IntakeDtos.AnswerInput> answers = validate(request);
		long responseId;
		try {
			responseId = repository.insertResponse(studentId, clock.instant());
		}
		catch (DuplicateKeyException e) {
			throw problem(HttpStatus.CONFLICT, "The intake was already submitted; edit it instead.");
		}
		return save(studentId, responseId, answers);
	}

	@Transactional
	public IntakeView edit(long studentId, IntakeDtos.IntakeRequest request) {
		Map<Question, IntakeDtos.AnswerInput> answers = validate(request);
		long responseId = repository.responseIdOf(studentId).orElseThrow(
				() -> problem(HttpStatus.NOT_FOUND, "No intake has been submitted yet."));
		repository.clearAnswers(responseId, clock.instant());
		return save(studentId, responseId, answers);
	}

	private IntakeView save(long studentId, long responseId, Map<Question, IntakeDtos.AnswerInput> answers) {
		Set<Long> interestTags = new LinkedHashSet<>();
		answers.forEach((q, a) -> {
			long answerId = repository.insertAnswer(responseId, q.id(), q.tags() ? null : a.text().strip());
			if (q.tags()) {
				for (Long tag : new LinkedHashSet<>(a.tagIds())) {
					repository.insertAnswerTag(answerId, tag);
					interestTags.add(tag);
				}
			}
		});
		events.publishEvent(new IntakeSaved(studentId, responseId, Set.copyOf(interestTags)));
		return findIntake(studentId).orElseThrow();
	}

	/** Validates against the active questions; returns only answered questions, with normalised input. */
	private Map<Question, IntakeDtos.AnswerInput> validate(IntakeDtos.IntakeRequest request) {
		Map<String, Question> byKey = new LinkedHashMap<>();
		activeQuestions().forEach(q -> byKey.put(q.key(), q));
		Set<Long> knownTags = new LinkedHashSet<>();
		catalog.allTags().forEach(t -> knownTags.add(t.id()));
		List<Map<String, String>> errors = new ArrayList<>();
		Map<Question, IntakeDtos.AnswerInput> result = new LinkedHashMap<>();
		request.answers().forEach((key, input) -> {
			Question q = byKey.get(key);
			String field = "answers[" + key + "]";
			if (q == null) {
				errors.add(Map.of("field", field, "message", "Unknown question."));
				return;
			}
			if (input == null) {
				return;
			}
			boolean hasText = input.text() != null && !input.text().isBlank();
			boolean hasTags = input.tagIds() != null && !input.tagIds().isEmpty();
			if (q.tags()) {
				if (input.text() != null && !input.text().isBlank()) {
					errors.add(Map.of("field", field + ".text", "message", "This question takes tagIds."));
				}
				else if (hasTags && !knownTags.containsAll(input.tagIds())) {
					errors.add(Map.of("field", field + ".tagIds", "message", "Unknown tag."));
				}
				else if (hasTags) {
					result.put(q, new IntakeDtos.AnswerInput(null, input.tagIds()));
				}
			}
			else {
				if (hasTags) {
					errors.add(Map.of("field", field + ".tagIds", "message", "This question takes text."));
				}
				else if (hasText && input.text().strip().length() > MAX_TEXT) {
					errors.add(Map.of("field", field + ".text",
							"message", "Must be at most " + MAX_TEXT + " characters."));
				}
				else if (hasText) {
					result.put(q, new IntakeDtos.AnswerInput(input.text(), List.of()));
				}
			}
		});
		if (errors.isEmpty() && result.isEmpty()) {
			errors.add(Map.of("field", "answers", "message", "Answer at least one question."));
		}
		if (!errors.isEmpty()) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed.");
			problem.setProperty("errors", errors);
			throw new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
		}
		return result;
	}

	@Transactional
	public void dismiss(long studentId) {
		try {
			repository.insertDismissalIfAbsent(studentId, clock.instant());
		}
		catch (DuplicateKeyException e) {
			// concurrent dismissal: already dismissed
		}
	}

	@Override
	public boolean intakePromptDue(long studentId) {
		return !hasCompletedIntake(studentId) && !repository.isDismissed(studentId)
				&& reflections.reflectionCount(studentId) >= minReflections;
	}

	@Override
	public boolean hasCompletedIntake(long studentId) {
		return repository.hasResponse(studentId);
	}

	@Override
	public Set<Long> studentsWithIntake(Collection<Long> studentIds) {
		return repository.studentsWithResponse(studentIds);
	}

	@Override
	public Map<Long, Instant> lastIntakeActivityOf(Collection<Long> studentIds) {
		return repository.lastUpdatedOf(studentIds);
	}

	@Override
	public Optional<IntakeView> findIntake(long studentId) {
		Optional<ResponseHead> head = repository.head(studentId);
		if (head.isEmpty()) {
			return Optional.empty();
		}
		Map<Long, TagView> tagById = new LinkedHashMap<>();
		catalog.allTags().forEach(t -> tagById.put(t.id(), t));
		Map<Long, List<TagView>> tagsByAnswer = new LinkedHashMap<>();
		repository.answerTags(head.get().id()).forEach(
				t -> tagsByAnswer.computeIfAbsent(t.answerId(), k -> new ArrayList<>()).add(tagById.get(t.tagId())));
		List<IntakeView.Answer> answers = repository.answers(head.get().id()).stream()
				.map(a -> new IntakeView.Answer(a.questionKey(), a.prompt(), a.questionType(), a.text(),
						tagsByAnswer.getOrDefault(a.id(), List.of())))
				.toList();
		return Optional.of(new IntakeView(head.get().id(), head.get().createdAt(), head.get().updatedAt(), answers));
	}

	private List<Question> activeQuestions() {
		return repository.activeQuestions();
	}

	private static ErrorResponseException problem(HttpStatus status, String detail) {
		return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
	}

}
