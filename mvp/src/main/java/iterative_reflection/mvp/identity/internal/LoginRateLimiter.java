package iterative_reflection.mvp.identity.internal;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory brute-force protection for sign-in [N2.5]: fixed 15-minute windows, counted per account (normalized
 * email) and per client IP. Once an account or an IP has the maximum failures in the current window, further
 * attempts (even with the right password) are refused until the next window. Counters reset on restart.
 */
@Component
class LoginRateLimiter {

	private static final long WINDOW_SECONDS = Duration.ofMinutes(15).toSeconds();

	private final Clock clock;

	private final int maxFailuresPerAccount;

	private final int maxFailuresPerIp;

	private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

	LoginRateLimiter(Clock clock, @Value("${identity.rate-limit.account-max-failures:5}") int maxFailuresPerAccount,
			@Value("${identity.rate-limit.ip-max-failures:5}") int maxFailuresPerIp) {
		this.clock = clock;
		this.maxFailuresPerAccount = maxFailuresPerAccount;
		this.maxFailuresPerIp = maxFailuresPerIp;
	}

	boolean isBlocked(String account, String ip) {
		return count("a:" + account) >= maxFailuresPerAccount || count("i:" + ip) >= maxFailuresPerIp;
	}

	void recordFailure(String account, String ip) {
		long window = window();
		counters.keySet().removeIf(key -> windowOf(key) < window);
		counters.computeIfAbsent(window + "|a:" + account, k -> new AtomicInteger()).incrementAndGet();
		counters.computeIfAbsent(window + "|i:" + ip, k -> new AtomicInteger()).incrementAndGet();
	}

	private int count(String key) {
		AtomicInteger counter = counters.get(window() + "|" + key);
		return counter == null ? 0 : counter.get();
	}

	private long window() {
		return clock.instant().getEpochSecond() / WINDOW_SECONDS;
	}

	private static long windowOf(String key) {
		return Long.parseLong(key.substring(0, key.indexOf('|')));
	}

}
