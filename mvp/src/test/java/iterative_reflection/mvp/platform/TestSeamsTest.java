package iterative_reflection.mvp.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/** The {@link Clock} and the random source are ordinary beans that tests can configure or replace. */
class TestSeamsTest {

	@Nested
	class Defaults extends HttpIntegrationTest {

		@Autowired
		Clock clock;

		@Autowired
		RandomGenerator random;

		@Test
		void clockIsAUtcSystemClock() {
			assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
			assertThat(clock.instant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
		}

		@Test
		void randomSourceIsAvailable() {
			assertThat(random.nextInt(10)).isBetween(0, 9);
		}

	}

	@Nested
	@TestPropertySource(properties = "platform.random.seed=42")
	class SeededRandom extends HttpIntegrationTest {

		@Autowired
		RandomGenerator random;

		@Test
		void seededRandomSourceIsDeterministic() {
			Random expected = new Random(42);
			int[] expectedDraws = IntStream.range(0, 5).map(i -> expected.nextInt(1000)).toArray();
			int[] actualDraws = IntStream.range(0, 5).map(i -> random.nextInt(1000)).toArray();
			assertThat(actualDraws).containsExactly(expectedDraws);
		}

	}

	@Nested
	class OverriddenClock extends HttpIntegrationTest {

		static final Instant FIXED = Instant.parse("2026-01-01T00:00:00Z");

		@TestConfiguration
		static class FixedClock {

			@Bean
			@Primary
			Clock fixedClock() {
				return Clock.fixed(FIXED, ZoneOffset.UTC);
			}

		}

		@Autowired
		Clock clock;

		@Test
		void testsCanReplaceTheClock() {
			assertThat(clock.instant()).isEqualTo(FIXED);
		}

	}

}
