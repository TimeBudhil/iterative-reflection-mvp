package iterative_reflection.mvp.feed.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tunable affinity numbers [R6.6]. */
@ConfigurationProperties("affinity")
record AffinityProperties(@DefaultValue("5.5") double neutralScore, @DefaultValue("4.5") double scale,
		@DefaultValue("0.5") double intakeBoost, @DefaultValue("-0.5") double abandonPenalty) {
}
