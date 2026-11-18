package iterative_reflection.mvp.counselor.internal.services;

import iterative_reflection.mvp.platform.FixedWindowCounter;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory limit on failed link attempts per student [R2.6]: fixed 15-minute windows. After
 * {@code max-failures} (default 10) failures in the current window the next attempt is refused (429).
 * Counters reset on restart.
 */
@Component
public class LinkAttemptLimiter {

	private final int maxFailures;

	private final FixedWindowCounter failures;

	LinkAttemptLimiter(Clock clock, @Value("${counselor.link-rate-limit.max-failures:10}") int maxFailures) {
		this.failures = new FixedWindowCounter(clock, Duration.ofMinutes(15));
		this.maxFailures = maxFailures;
	}

	public boolean isBlocked(long studentId) {
		return failures.count(Long.toString(studentId)) >= maxFailures;
	}

	public void recordFailure(long studentId) {
		failures.increment(Long.toString(studentId));
	}

}
