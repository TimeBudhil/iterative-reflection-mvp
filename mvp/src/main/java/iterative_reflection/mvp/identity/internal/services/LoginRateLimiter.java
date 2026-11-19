package iterative_reflection.mvp.identity.internal.services;

import iterative_reflection.mvp.platform.FixedWindowCounter;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory brute-force protection for sign-in [N2.5]: fixed 15-minute windows, counted per account (normalized
 * email) and per client IP. Once an account or an IP has the maximum failures in the current window, further
 * attempts (even with the right password) are refused until the next window. Counters reset on restart.
 */
@Component
public class LoginRateLimiter {

	private final int maxFailuresPerAccount;

	private final int maxFailuresPerIp;

	private final FixedWindowCounter failures;

	LoginRateLimiter(Clock clock, @Value("${identity.rate-limit.account-max-failures:5}") int maxFailuresPerAccount,
			@Value("${identity.rate-limit.ip-max-failures:5}") int maxFailuresPerIp) {
		this.failures = new FixedWindowCounter(clock, Duration.ofMinutes(15));
		this.maxFailuresPerAccount = maxFailuresPerAccount;
		this.maxFailuresPerIp = maxFailuresPerIp;
	}

	public boolean isBlocked(String account, String ip) {
		return failures.count("a:" + account) >= maxFailuresPerAccount || failures.count("i:" + ip) >= maxFailuresPerIp;
	}

	public void recordFailure(String account, String ip) {
		failures.increment("a:" + account);
		failures.increment("i:" + ip);
	}

}
