package iterative_reflection.mvp.intake.internal;

import iterative_reflection.mvp.career.TagView;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

final class IntakeDtos {

	private IntakeDtos() {
	}

	/** {@code options} is non-null only for TAGS questions. */
	record QuestionView(long id, String key, String prompt, String type, List<TagView> options) {
	}

	/** Answers keyed by question key; TAGS questions take {@code tagIds}, TEXT questions take {@code text}. */
	record IntakeRequest(@NotNull Map<String, AnswerInput> answers) {
	}

	record AnswerInput(String text, List<Long> tagIds) {
	}

}
