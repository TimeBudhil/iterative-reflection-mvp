package iterative_reflection.mvp.platform;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory counters per key over fixed time windows (the window number is {@code epochSecond / windowSeconds}).
 * Counts of older windows are dropped on the next increment, and everything resets on restart. Shared by the
 * brute-force limiters (sign-in, counselor link attempts).
 */
public class FixedWindowCounter {

	private final Clock clock;

	private final long windowSeconds;

	private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	public FixedWindowCounter(Clock clock, Duration window) {
		this.clock = clock;
		this.windowSeconds = window.toSeconds();
	}

	/** Failures recorded for the key in the current window. */
	public int count(String key) {
		AtomicInteger counter = counters.get(window() + "|" + key);
		return counter == null ? 0 : counter.get();
	}

	public void increment(String key) {
		long window = window();
		counters.keySet().removeIf(k -> Long.parseLong(k.substring(0, k.indexOf('|'))) < window);
		counters.computeIfAbsent(window + "|" + key, k -> new AtomicInteger()).incrementAndGet();
	}

	private long window() {
		return clock.instant().getEpochSecond() / windowSeconds;
	}

}
