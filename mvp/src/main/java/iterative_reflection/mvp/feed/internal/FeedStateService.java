package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.feed.FeedCard;
import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.feed.TagAffinity;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Deliberately not {@code @Transactional}: every statement is atomic, and a lost race on the primary key is resolved
 * by re-reading the winner's card (a failed insert would poison a surrounding Postgres transaction).
 */
@Service
class FeedStateService implements FeedService {

	private final JdbcClient jdbc;

	private final CareerCatalog catalog;

	private final ReflectionReader reflections;

	private final CardPicker picker;

	private final Clock clock;

	private final AffinityApplier affinity;

	FeedStateService(JdbcClient jdbc, CareerCatalog catalog, ReflectionReader reflections, CardPicker picker,
			Clock clock, AffinityApplier affinity) {
		this.affinity = affinity;
		this.jdbc = jdbc;
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
			jdbc.sql("insert into feed_state (student_id, career_id, served_at) values (:s, :c, :t)")
					.param("s", studentId).param("c", picked.get().id())
					.param("t", Timestamp.from(clock.instant())).update();
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

	/** Candidate pool: active careers the student has not reflected on, in default order. Ticket 12 adds abandons. */
	List<CareerSummary> candidatePool(long studentId, Set<Long> reflected) {
		return catalog.activeCareersInDefaultOrder().stream().filter(c -> !reflected.contains(c.id())).toList();
	}

	void clear(long studentId, long careerId) {
		jdbc.sql("delete from feed_state where student_id = :s and career_id = :c").param("s", studentId)
				.param("c", careerId).update();
	}

	private Optional<Long> storedCareerId(long studentId) {
		return jdbc.sql("select career_id from feed_state where student_id = :s").param("s", studentId)
				.query(Long.class).optional();
	}

}
