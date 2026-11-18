package iterative_reflection.mvp.counselor.internal.services;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory limit on failed link attempts per student [R2.6]: fixed 15-minute windows. After
 * {@code max-failures} (default 10) failures in the current window the next attempt is refused (429).
 * Counters reset on restart.
 */
@Component
public class LinkAttemptLimiter {

	private static final long WINDOW_SECONDS = Duration.ofMinutes(15).toSeconds();

	private final Clock clock;

	private final int maxFailures;

	private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	LinkAttemptLimiter(Clock clock, @Value("${counselor.link-rate-limit.max-failures:10}") int maxFailures) {
		this.clock = clock;
		this.maxFailures = maxFailures;
	}

	public boolean isBlocked(long studentId) {
		AtomicInteger c = counters.get(window() + "|" + studentId);
		return c != null && c.get() >= maxFailures;
	}

	public void recordFailure(long studentId) {
		long window = window();
		counters.keySet().removeIf(key -> Long.parseLong(key.substring(0, key.indexOf('|'))) < window);
		counters.computeIfAbsent(window + "|" + studentId, k -> new AtomicInteger()).incrementAndGet();
	}

	private long window() {
		return clock.instant().getEpochSecond() / WINDOW_SECONDS;
	}

}
