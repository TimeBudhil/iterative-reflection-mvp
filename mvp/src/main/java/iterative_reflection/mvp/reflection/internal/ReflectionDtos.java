package iterative_reflection.mvp.reflection.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request and response shapes of the reflection endpoints. */
final class ReflectionDtos {

	private ReflectionDtos() {
	}

	enum Verdict {
		YES, MAYBE, NO
	}

	record AnswerInput(
			@NotNull(message = "A score from 1 to 10 is required.") @Min(value = 1, message = "Score must be between 1 and 10.") @Max(value = 10, message = "Score must be between 1 and 10.") Integer score,
			@Size(max = 1000, message = "Open answer must be at most 1000 characters.") String openText) {
	}

	/** {@code answers} is keyed by question key, e.g. {@code {"interest": {"score": 8, "openText": "..."}}}. */
	record SubmitRequest(
			@NotNull(message = "Answers are required.") Map<String, @NotNull(message = "An answer is required.") @Valid AnswerInput> answers,
			@NotNull(message = "A verdict of YES, MAYBE or NO is required.") Verdict verdict) {
	}

	record QuestionView(long id, String key, int version, String prompt, String openPrompt, int minScore,
			int maxScore) {
	}

	record AnswerView(String key, int questionVersion, String prompt, int score, String openText) {
	}

	record ReflectionView(long id, long careerId, String careerTitle, Verdict verdict, int versionNumber,
			List<AnswerView> answers, Instant createdAt, Instant updatedAt) {
	}

	record VersionView(int versionNumber, Verdict verdict, List<AnswerView> answers, Instant createdAt) {
	}

	record SubmitResponse(long id, long careerId, Verdict verdict, int versionNumber) {
	}

}
