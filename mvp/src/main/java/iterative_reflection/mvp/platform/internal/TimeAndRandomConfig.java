package iterative_reflection.mvp.platform.internal;

import java.time.Clock;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Injectable time and randomness. Code that needs "now" injects {@link Clock}; code that needs randomness (the feed's
 * exploration slot) injects {@link RandomGenerator}. Tests replace either with a {@code @Primary} bean, or seed the
 * random source with {@code platform.random.seed}.
 */
@Configuration(proxyBeanMethods = false)
class TimeAndRandomConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	RandomGenerator randomGenerator(@Value("${platform.random.seed:#{null}}") Long seed) {
		return seed == null ? new Random() : new Random(seed);
	}

}
