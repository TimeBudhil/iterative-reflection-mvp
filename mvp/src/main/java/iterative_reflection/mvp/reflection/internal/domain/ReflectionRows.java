package iterative_reflection.mvp.reflection.internal.domain;

import java.time.Instant;

/** Plain read models returned by the reflection repository (mapped to DTOs and public records by the services). */
public final class ReflectionRows {

	private ReflectionRows() {
	}

	public record QuestionRow(long id, String key, int version, String prompt, String openPrompt) {
	}

	public record VersionHeader(long id, int number, String verdict, Instant createdAt) {
	}

	public record ReflectionHeader(long id, long careerId, int versionNumber, String verdict, Instant createdAt,
			Instant updatedAt, long versionId) {
	}

	public record AnswerRow(long versionId, String key, int questionVersion, String prompt, int score,
			String openText) {
	}

	public record ActivityRow(long studentId, int total, int yes, int maybe, int no, Instant lastActivity) {
	}

	/** Pilot aggregates over a cohort: students with enough reflections, and current-version verdict counts. */
	public record CohortStats(long studentsWithMin, long yes, long maybe, long no) {
	}

}
