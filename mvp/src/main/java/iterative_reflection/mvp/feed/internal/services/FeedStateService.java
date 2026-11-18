package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.CardPicker;
import iterative_reflection.mvp.feed.internal.repository.AbandonRepository;
import iterative_reflection.mvp.feed.internal.repository.FeedStateRepository;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.feed.AbandonActivity;
import iterative_reflection.mvp.feed.AbandonedCareer;
import iterative_reflection.mvp.feed.FeedCard;
import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.feed.TagAffinity;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Deliberately not {@code @Transactional}: every statement is atomic, and a lost race on the primary key is resolved
 * by re-reading the winner's card (a failed insert would poison a surrounding Postgres transaction).
 */
@Service
public class FeedStateService implements FeedService {

	private final FeedStateRepository feedState;

	private final AbandonRepository abandonRows;

	private final CareerCatalog catalog;

	private final ReflectionReader reflections;

	private final CardPicker picker;

	private final Clock clock;

	private final AffinityApplier affinity;

	private final AbandonService abandons;

	FeedStateService(FeedStateRepository feedState, AbandonRepository abandonRows, CareerCatalog catalog, ReflectionReader reflections, CardPicker picker,
			Clock clock, AffinityApplier affinity, AbandonService abandons) {
		this.abandons = abandons;
		this.affinity = affinity;
		this.feedState = feedState;
		this.abandonRows = abandonRows;
		this.catalog = catalog;
		this.reflections = reflections;
		this.picker = picker;
		this.clock = clock;
	}

	@Override
	public FeedCard next(long studentId) {
		Set<Long> reflected = reflections.reflectedCareerIds(studentId);
		Optional<Long> stored = storedCareerId(studentId);
		if (stored.isPresent()) {
			long id = stored.get();
			Optional<CareerSummary> valid = catalog.findCareer(id)
					.filter(c -> c.active() && !reflected.contains(id));
			if (valid.isPresent()) {
				return FeedCard.of(valid.get());
			}
			clear(studentId, id);
		}
		Optional<CareerSummary> picked = picker.pick(studentId, candidatePool(studentId, reflected));
		if (picked.isEmpty()) {
			return FeedCard.noneLeft();
		}
		try {
			feedState.insert(studentId, picked.get().id(), clock.instant());
			return FeedCard.of(picked.get());
		}
		catch (DuplicateKeyException lostRace) {
			return storedCareerId(studentId).flatMap(catalog::findCareer).map(FeedCard::of)
					.orElseGet(() -> next(studentId));
		}
	}

	@Override
	public List<TagAffinity> topAffinities(long studentId, int n) {
		return affinity.top(studentId, n);
	}

	@Override
	public OptionalLong inProgressCareerId(long studentId) {
		Set<Long> reflected = reflections.reflectedCareerIds(studentId);
		return storedCareerId(studentId).filter(id -> !reflected.contains(id))
				.filter(id -> catalog.findCareer(id).map(CareerSummary::active).orElse(false))
				.map(OptionalLong::of).orElse(OptionalLong.empty());
	}

	@Override
	public void abandon(long studentId, long careerId) {
		abandons.abandon(studentId, careerId);
	}

	@Override
	public Map<Long, AbandonActivity> abandonActivityOf(java.util.Collection<Long> studentIds) {
		Map<Long, AbandonActivity> result = new java.util.HashMap<>();
		if (!studentIds.isEmpty()) {
			abandonRows.summaryOf(studentIds).forEach(
					s -> result.put(s.studentId(), new AbandonActivity(s.total(), s.lastAbandonedAt())));
		}
		return result;
	}

	@Override
	public List<AbandonedCareer> abandonedCareers(long studentId) {
		return abandons.abandoned(studentId);
	}

	/**
	 * Candidate pool in default order: unseen active careers (not reflected, never abandoned); when none are left,
	 * careers abandoned once. Careers abandoned twice are gone for good [R4.8].
	 */
	List<CareerSummary> candidatePool(long studentId, Set<Long> reflected) {
		Map<Long, Integer> abandoned = abandons.abandonCounts(studentId);
		List<CareerSummary> open = catalog.activeCareersInDefaultOrder().stream()
				.filter(c -> !reflected.contains(c.id())).toList();
		List<CareerSummary> unseen = open.stream().filter(c -> !abandoned.containsKey(c.id())).toList();
		if (!unseen.isEmpty()) {
			return unseen;
		}
		return open.stream().filter(c -> abandoned.getOrDefault(c.id(), 0) == 1).toList();
	}

	void clear(long studentId, long careerId) {
		feedState.delete(studentId, careerId);
	}

	private Optional<Long> storedCareerId(long studentId) {
		return feedState.storedCareerId(studentId);
	}

}
