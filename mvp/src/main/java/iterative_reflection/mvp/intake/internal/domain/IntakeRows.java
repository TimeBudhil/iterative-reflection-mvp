package iterative_reflection.mvp.intake.internal.domain;

import java.time.Instant;

/** Plain read models returned by the intake repository. */
public final class IntakeRows {

	private IntakeRows() {
	}

	public record Question(long id, String key, String prompt, String type) {

		public boolean tags() {
			return "TAGS".equals(type);
		}

	}

	public record ResponseHead(long id, Instant createdAt, Instant updatedAt) {
	}

	public record AnswerTag(long answerId, long tagId) {
	}

	public record AnswerRow(long id, String questionKey, String prompt, String questionType, String text) {
	}

}
