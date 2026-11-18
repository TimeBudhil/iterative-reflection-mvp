package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.career.CareerSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Picks the first career in catalog default order. */
@Component
class DefaultOrderPicker implements CardPicker {

	@Override
	public Optional<CareerSummary> pick(long studentId, List<CareerSummary> pool) {
		return pool.stream().findFirst();
	}

}
