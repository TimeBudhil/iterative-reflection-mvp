package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.career.CareerSummary;
import java.util.List;
import java.util.Optional;

/**
 * Strategy that chooses the next card from a candidate pool (already in catalog default order; may be empty).
 * Implemented by {@link AffinityPicker}; ticket 12 changes only
 * how the pool is built in {@link FeedStateService#candidatePool}.
 */
interface CardPicker {

	Optional<CareerSummary> pick(long studentId, List<CareerSummary> pool);

}
