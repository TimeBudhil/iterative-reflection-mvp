package iterative_reflection.mvp.reflection;

import java.time.Instant;

/** A student's reflection totals: current-version verdict counts and the latest reflection or edit time. */
public record ReflectionActivity(int reflected, int yes, int maybe, int no, Instant lastActivity) {
}
