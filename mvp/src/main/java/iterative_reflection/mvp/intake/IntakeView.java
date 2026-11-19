package iterative_reflection.mvp.intake;

import iterative_reflection.mvp.career.TagView;
import java.time.Instant;
import java.util.List;

/**
 * A student's intake answers. Only answered questions are present. {@code text} is set for TEXT questions and
 * {@code tags} for TAGS questions (empty otherwise).
 */
public record IntakeView(long id, Instant submittedAt, Instant updatedAt, List<Answer> answers) {

	public record Answer(String key, String prompt, String type, String text, List<TagView> tags) {
	}

}
