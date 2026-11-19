package iterative_reflection.mvp.feed;

import iterative_reflection.mvp.career.CareerSummary;

/** The card served by the feed; {@code career} is null exactly when {@code exhausted}. */
public record FeedCard(CareerSummary career, boolean exhausted) {

	public static FeedCard of(CareerSummary career) {
		return new FeedCard(career, false);
	}

	public static FeedCard noneLeft() {
		return new FeedCard(null, true);
	}

}
