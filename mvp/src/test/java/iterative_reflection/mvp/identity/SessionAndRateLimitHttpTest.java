package iterative_reflection.mvp.identity;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/** Sign-in rate limiting [N2.5] and session timeouts [N2.2], driven by a movable {@link Clock} (no sleeping). */
@TestPropertySource(properties = "identity.rate-limit.ip-max-failures=5")
class SessionAndRateLimitHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@TestConfiguration
	static class ClockConfig {

		@Bean
		@Primary
		MovableClock movableClock() {
			return new MovableClock();
		}

	}

	static class MovableClock extends Clock {

		private volatile Instant now = Instant.parse("2030-01-01T12:00:00Z");

		void advance(Duration d) {
			now = now.plus(d);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

	@Autowired
	MovableClock clock;

	@Test
	void sixthFailedSigninForTheSameAccountIs429AndWindowExpiryAllowsSigninAgain() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		// move into a fresh fixed 15-minute window so earlier tests' failures do not count
		clock.advance(Duration.ofMinutes(15));
		for (int i = 0; i < 5; i++) {
			signin(email, "wrong password!!", null).expectStatus().isUnauthorized();
		}
		signin(email, "wrong password!!", null).expectStatus().isEqualTo(429)
				.expectHeader().contentTypeCompatibleWith("application/problem+json");
		// even the right password is refused while blocked
		signin(email, PASSWORD, null).expectStatus().isEqualTo(429);

		clock.advance(Duration.ofMinutes(15));
		signin(email, PASSWORD, null).expectStatus().isOk();
	}

	@Test
	void failuresAgainstDifferentAccountsFromOneIpAlsoTripTheLimit() {
		clock.advance(Duration.ofMinutes(15));
		for (int i = 0; i < 5; i++) {
			signin(uniqueEmail(), "wrong password!!", null).expectStatus().isUnauthorized();
		}
		signin(uniqueEmail(), "wrong password!!", null).expectStatus().isEqualTo(429);
		clock.advance(Duration.ofMinutes(15));
		signin(uniqueEmail(), "wrong password!!", null).expectStatus().isUnauthorized();
	}

	@Test
	void sessionIdleFor30MinutesIs401ButActivityKeepsItAlive() {
		clock.advance(Duration.ofMinutes(15));
		Session session = signedInStudent();
		clock.advance(Duration.ofMinutes(29));
		http.get().uri("/api/me").headers(session::apply).exchange().expectStatus().isOk();
		clock.advance(Duration.ofMinutes(29));
		http.get().uri("/api/me").headers(session::apply).exchange().expectStatus().isOk();
		clock.advance(Duration.ofMinutes(31));
		http.get().uri("/api/me").headers(session::apply).exchange().expectStatus().isUnauthorized();
	}

	@Test
	void sessionOlderThan12HoursIs401EvenIfActive() {
		clock.advance(Duration.ofMinutes(15));
		Session session = signedInStudent();
		for (int i = 0; i < 28; i++) {
			clock.advance(Duration.ofMinutes(25));
			http.get().uri("/api/me").headers(session::apply).exchange().expectStatus().isOk();
		}
		clock.advance(Duration.ofMinutes(19));
		http.get().uri("/api/me").headers(session::apply).exchange().expectStatus().isOk();
		clock.advance(Duration.ofMinutes(2));
		http.get().uri("/api/me").headers(session::apply).exchange().expectStatus().isUnauthorized();
	}

}
