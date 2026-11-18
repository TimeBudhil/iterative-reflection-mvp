package iterative_reflection.mvp.feed.internal.services;

import iterative_reflection.mvp.feed.internal.domain.AffinitySource;
import iterative_reflection.mvp.feed.internal.domain.AffinityProperties;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.feed.AbandonedCareer;
import iterative_reflection.mvp.feed.CareerAbandoned;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
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

	private final JdbcClient jdbc;

	private final CareerCatalog catalog;

	private final ReflectionReader reflections;

	private final AffinityApplier affinity;

	private final AffinityProperties props;

	private final Clock clock;

	private final ApplicationEventPublisher events;

	AbandonService(JdbcClient jdbc, CareerCatalog catalog, ReflectionReader reflections, AffinityApplier affinity,
			AffinityProperties props, Clock clock, ApplicationEventPublisher events) {
		this.jdbc = jdbc;
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
		Timestamp now = Timestamp.from(clock.instant());
		if (previous == 0) {
			jdbc.sql("""
					insert into abandons (student_id, career_id, last_viewed_page, abandon_count, abandoned_at)
					values (:s, :c, :p, 1, :t)""").param("s", studentId).param("c", careerId).param("p", lastViewed)
					.param("t", now).update();
		}
		else {
			jdbc.sql("""
					update abandons set last_viewed_page = :p, abandon_count = 2, abandoned_at = :t
					where student_id = :s and career_id = :c""").param("s", studentId).param("c", careerId)
					.param("p", lastViewed).param("t", now).update();
		}
		long abandonId = jdbc.sql("select id from abandons where student_id = :s and career_id = :c")
				.param("s", studentId).param("c", careerId).query(Long.class).single();
		affinity.replaceForCareer(studentId, AffinitySource.ABANDON, abandonId, careerId,
				props.abandonPenalty() * (previous + 1));
		jdbc.sql("delete from feed_state where student_id = :s and career_id = :c").param("s", studentId)
				.param("c", careerId).update();
		events.publishEvent(new CareerAbandoned(studentId, career.id()));
	}

	/** Career id to abandon count (1 or 2). */
	Map<Long, Integer> abandonCounts(long studentId) {
		Map<Long, Integer> out = new HashMap<>();
		jdbc.sql("select career_id, abandon_count from abandons where student_id = :s").param("s", studentId)
				.query((rs, i) -> {
					out.put(rs.getLong("career_id"), rs.getInt("abandon_count"));
					return null;
				}).list();
		return out;
	}

	List<AbandonedCareer> abandoned(long studentId) {
		return jdbc.sql("""
				select a.career_id, c.title, a.last_viewed_page, a.abandon_count, a.abandoned_at
				from abandons a join careers c on c.id = a.career_id
				where a.student_id = :s order by a.abandoned_at desc, a.id desc""").param("s", studentId)
				.query((rs, i) -> new AbandonedCareer(rs.getLong("career_id"), rs.getString("title"),
						rs.getInt("last_viewed_page"), rs.getInt("abandon_count"),
						rs.getTimestamp("abandoned_at").toInstant()))
				.list();
	}

	@Override
	public long abandons(long studentId) {
		return jdbc.sql("select coalesce(sum(abandon_count), 0) from abandons where student_id = :s")
				.param("s", studentId).query(Long.class).single();
	}

}
