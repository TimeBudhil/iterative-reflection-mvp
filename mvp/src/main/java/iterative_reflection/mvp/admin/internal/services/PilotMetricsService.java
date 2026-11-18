package iterative_reflection.mvp.admin.internal.services;

import iterative_reflection.mvp.admin.internal.dto.PilotMetrics;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.intake.IntakeReader;
import iterative_reflection.mvp.reflection.PilotReflectionStats;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Computes pilot metrics from the other modules' aggregate reads (a handful of count queries, never per-student
 * loads). The all-time result backing the Prometheus gauges is cached for {@code pilot.metrics.cache-ttl} so a
 * scrape never costs more than one refresh per interval.
 */
@Service
public class PilotMetricsService {

	static final int MIN_REFLECTIONS = 3;

	private final UserService users;

	private final ReflectionReader reflections;

	private final IntakeReader intake;

	private final Duration ttl;

	private volatile PilotMetrics cached;

	private volatile long cachedAtNanos;

	PilotMetricsService(UserService users, ReflectionReader reflections, IntakeReader intake,
			@Value("${pilot.metrics.cache-ttl:PT30S}") Duration ttl) {
		this.users = users;
		this.reflections = reflections;
		this.intake = intake;
		this.ttl = ttl;
	}

	/** Signup dates are UTC calendar days, both inclusive; null = open. */
	public PilotMetrics compute(LocalDate from, LocalDate to) {
		Instant start = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant end = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		Set<Long> cohort = users.pilotStudentIds(start, end);
		long total = cohort.size();
		PilotReflectionStats r = reflections.pilotStats(MIN_REFLECTIONS, cohort);
		long withIntake = intake.studentsWithIntake(cohort).size();
		return new PilotMetrics(total, r.studentsWithMinReflections(), percent(r.studentsWithMinReflections(), total),
				withIntake, percent(withIntake, total), new PilotMetrics.Verdicts(r.yes(), r.maybe(), r.no()));
	}

	/** All-time metrics, at most {@code ttl} old. */
	public PilotMetrics allTimeCached() {
		PilotMetrics current = cached;
		if (current == null || System.nanoTime() - cachedAtNanos >= ttl.toNanos()) {
			synchronized (this) {
				current = compute(null, null);
				cached = current;
				cachedAtNanos = System.nanoTime();
			}
		}
		return current;
	}

	private static double percent(long part, long whole) {
		return whole == 0 ? 0 : Math.round(part * 10000.0 / whole) / 100.0;
	}

}
