package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.career.CareerSummary;
import java.util.List;
import java.util.Optional;

/**
 * Strategy that chooses the next card from a candidate pool (already in catalog default order; may be empty).
 * Ticket 10 replaces {@link DefaultOrderPicker} with affinity ranking plus the exploration slot; ticket 12 changes only
 * how the pool is built in {@link FeedStateService#candidatePool}.
 */
interface CardPicker {

	Optional<CareerSummary> pick(long studentId, List<CareerSummary> pool);

}
