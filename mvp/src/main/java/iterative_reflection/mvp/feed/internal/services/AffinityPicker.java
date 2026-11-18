package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.CardPicker;
import iterative_reflection.mvp.feed.internal.domain.FeedProperties;

import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Highest summed tag affinity (ties: first in the pool, which is catalog default order), except that when
 * (reflections + abandons) mod exploration-every is the last position, a random pool member is chosen [R4.4, R4.5].
 * The slot counter is derived, never stored.
 */
@Component
public class AffinityPicker implements CardPicker {

	private static final double EPSILON = 1e-9;

	private final AffinityApplier affinity;

	private final ReflectionReader reflections;

	private final AbandonCounter abandons;

	private final FeedProperties props;

	private final RandomGenerator random;

	AffinityPicker(AffinityApplier affinity, ReflectionReader reflections, AbandonCounter abandons,
			FeedProperties props, RandomGenerator random) {
		this.affinity = affinity;
		this.reflections = reflections;
		this.abandons = abandons;
		this.props = props;
		this.random = random;
	}

	@Override
	public Optional<CareerSummary> pick(long studentId, List<CareerSummary> pool) {
		if (pool.isEmpty()) {
			return Optional.empty();
		}
		long count = reflections.reflectedCareerIds(studentId).size() + abandons.abandons(studentId);
		int every = Math.max(props.explorationEvery(), 1);
		if (count % every == every - 1) {
			return Optional.of(pool.get(random.nextInt(pool.size())));
		}
		Map<Long, Double> scores = affinity.scores(studentId);
		CareerSummary best = null;
		double bestScore = 0;
		for (CareerSummary c : pool) {
			double s = c.tags().stream().mapToDouble(t -> scores.getOrDefault(t.id(), 0.0)).sum();
			if (best == null || s > bestScore + EPSILON) {
				best = c;
				bestScore = s;
			}
		}
		return Optional.of(best);
	}

}
