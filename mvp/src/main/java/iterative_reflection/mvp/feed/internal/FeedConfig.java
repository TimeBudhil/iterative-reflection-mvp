package iterative_reflection.mvp.feed.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ AffinityProperties.class, FeedProperties.class })
class FeedConfig {
}
