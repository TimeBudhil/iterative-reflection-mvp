package iterative_reflection.mvp.reflection;

import java.time.Instant;
import java.util.List;

/** A reflection at its current version, for read-only views (counselor dashboard). */
public record ReflectionRecord(long id, long careerId, String careerTitle, String verdict, int versionNumber,
		List<Answer> answers, Instant createdAt, Instant updatedAt) {

	public record Answer(String key, int questionVersion, String prompt, int score, String openText) {
	}

}
