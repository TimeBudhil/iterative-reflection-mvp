package iterative_reflection.mvp.feed.internal.integration;

import iterative_reflection.mvp.feed.internal.domain.FeedProperties;
import iterative_reflection.mvp.feed.internal.domain.AffinityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ AffinityProperties.class, FeedProperties.class })
public class FeedConfig {
}
