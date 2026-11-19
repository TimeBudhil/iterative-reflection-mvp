package iterative_reflection.mvp.feed.internal.domain;

import java.time.Instant;

/** Plain read models returned by the feed repositories. */
public final class FeedRows {

	private FeedRows() {
	}

	public record AbandonRow(long careerId, int lastViewedPage, int abandonCount, Instant abandonedAt) {
	}

	public record AbandonSummary(long studentId, int total, Instant lastAbandonedAt) {
	}

	public record TagScore(long tagId, double score) {
	}

}
