package iterative_reflection.mvp.feed;

import java.time.Instant;

/** Number of careers the student abandoned and when they last abandoned one. */
public record AbandonActivity(int abandonedCareers, Instant lastAbandonedAt) {
}
