package iterative_reflection.mvp.career;

import java.time.Instant;

/** How many careers a student has started (viewed at least one page of) and when they last viewed a new page. */
public record ViewActivity(int careersSeen, Instant lastViewedAt) {
}
