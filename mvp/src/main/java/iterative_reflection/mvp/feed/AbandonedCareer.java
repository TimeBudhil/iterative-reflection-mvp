package iterative_reflection.mvp.feed;

import java.time.Instant;

/**
 * A career the student abandoned: how far they got ({@code lastViewedPage} at the latest abandon), how many times
 * (1 = may resurface, 2 = permanently out of the feed) and when.
 */
public record AbandonedCareer(long careerId, String title, int lastViewedPage, int abandonCount,
		Instant abandonedAt) {
}
