package iterative_reflection.mvp.feed.internal.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** {@code feed.exploration-every}: every Nth card is a random exploration pick [R4.4]. */
@ConfigurationProperties("feed")
public record FeedProperties(@DefaultValue("5") int explorationEvery) {
}
