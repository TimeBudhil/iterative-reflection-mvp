package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.AffinitySource;
import iterative_reflection.mvp.feed.internal.repository.AbandonRepository;
import iterative_reflection.mvp.feed.internal.repository.FeedStateRepository;
import iterative_reflection.mvp.feed.internal.domain.AffinityProperties;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.feed.AbandonedCareer;
import iterative_reflection.mvp.feed.CareerAbandoned;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Abandon records (one row per student and career; {@code abandon_count} 2 = final). The penalty is written as
 * {@code abandon-penalty x abandon_count}, replacing the previous contribution, so a second abandon applies the
 * penalty again [R4.7, R4.8, R6.7].
 */
@Service
public class AbandonService implements AbandonCounter {

	private final AbandonRepository abandonRows;

	private final FeedStateRepository feedState;

	private final CareerCatalog catalog;

	private final ReflectionReader reflections;

	private final AffinityApplier affinity;

	private final AffinityProperties props;

	private final Clock clock;

	private final ApplicationEventPublisher events;

	AbandonService(AbandonRepository abandonRows, FeedStateRepository feedState, CareerCatalog catalog, ReflectionReader reflections, AffinityApplier affinity,
			AffinityProperties props, Clock clock, ApplicationEventPublisher events) {
		this.abandonRows = abandonRows;
		this.feedState = feedState;
		this.catalog = catalog;
		this.reflections = reflections;
		this.affinity = affinity;
		this.props = props;
		this.clock = clock;
		this.events = events;
	}

	@Transactional
	void abandon(long studentId, long careerId) {
		CareerSummary career = catalog.findCareer(careerId).filter(CareerSummary::active)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		int lastViewed = catalog.lastViewedPage(studentId, careerId);
		if (lastViewed < 1) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Open the career before abandoning it.");
		}
		if (reflections.reflectedCareerIds(studentId).contains(careerId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already reflected on this career.");
		}
		int previous = abandonCounts(studentId).getOrDefault(careerId, 0);
		if (previous >= 2) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already abandoned this career.");
		}
		Instant now = clock.instant();
		if (previous == 0) {
			abandonRows.insertFirst(studentId, careerId, lastViewed, now);
		}
		else {
			abandonRows.markFinal(studentId, careerId, lastViewed, now);
		}
		long abandonId = abandonRows.idOf(studentId, careerId);
		affinity.replaceForCareer(studentId, AffinitySource.ABANDON, abandonId, careerId,
				props.abandonPenalty() * (previous + 1));
		feedState.delete(studentId, careerId);
		events.publishEvent(new CareerAbandoned(studentId, career.id()));
	}

	/** Career id to abandon count (1 or 2). */
	Map<Long, Integer> abandonCounts(long studentId) {
		return abandonRows.counts(studentId);
	}

	List<AbandonedCareer> abandoned(long studentId) {
		return abandonRows.rows(studentId).stream()
				.map(r -> new AbandonedCareer(r.careerId(),
						catalog.findCareer(r.careerId()).map(CareerSummary::title).orElse(""), r.lastViewedPage(),
						r.abandonCount(), r.abandonedAt()))
				.toList();
	}

	@Override
	public long abandons(long studentId) {
		return abandonRows.totalAbandons(studentId);
	}

}
