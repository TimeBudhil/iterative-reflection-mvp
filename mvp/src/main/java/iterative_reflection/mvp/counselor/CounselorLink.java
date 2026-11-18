package iterative_reflection.mvp.counselor;

import java.time.Instant;

/** An active student-counselor link, with the counselor's display data. {@code counselorActive} is false for a deactivated counselor. */
public record CounselorLink(long id, long counselorId, String displayName, boolean counselorActive, Instant linkedAt) {
}
