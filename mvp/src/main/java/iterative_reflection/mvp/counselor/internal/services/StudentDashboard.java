package iterative_reflection.mvp.counselor.internal.services;

import iterative_reflection.mvp.counselor.internal.repository.CounselorLinkRepository;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.ViewActivity;
import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.feed.AbandonActivity;
import iterative_reflection.mvp.feed.AbandonedCareer;
import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.feed.TagAffinity;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import iterative_reflection.mvp.intake.IntakeReader;
import iterative_reflection.mvp.intake.IntakeView;
import iterative_reflection.mvp.reflection.ReflectionActivity;
import iterative_reflection.mvp.reflection.ReflectionReader;
import iterative_reflection.mvp.reflection.ReflectionRecord;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Read-only assembly of the counselor's views from other modules' public read APIs. Deliberately not
 * {@code @Transactional}: the audit row is written in its own transaction and must never be tied to the read.
 */
@Service
public class StudentDashboard {

	static final int TOP_AFFINITIES = 5;

	public record Row(long studentId, String displayName, int careersSeen, int reflected, int abandoned, int yes, int maybe,
			int no, boolean intakeCompleted, Instant lastActive) {
	}

	/** The full read-only picture of one student. */
	public record Detail(long studentId, String displayName, List<ReflectionRecord> reflections,
			List<AbandonedCareer> abandonedCareers, List<TagAffinity> topAffinities, IntakeView intake) {
	}

	private final CounselorLinkRepository links;

	private final CounselorService counselors;

	private final AuditLog audit;

	private final UserService users;

	private final CareerCatalog careers;

	private final ReflectionReader reflections;

	private final FeedService feed;

	private final IntakeReader intake;

	StudentDashboard(CounselorLinkRepository links, CounselorService counselors, AuditLog audit, UserService users,
			CareerCatalog careers, ReflectionReader reflections, FeedService feed, IntakeReader intake) {
		this.links = links;
		this.counselors = counselors;
		this.audit = audit;
		this.users = users;
		this.careers = careers;
		this.reflections = reflections;
		this.feed = feed;
		this.intake = intake;
	}

	/**
	 * One student's full history. Always writes an audit row first (own transaction, so a 403 still leaves its row),
	 * then answers 403 unless the counselor may see the student. An unknown student id is indistinguishable from an
	 * unlinked one.
	 */
	public Detail detail(long counselorId, long studentId) {
		boolean allowed = counselors.canSee(counselorId, studentId);
		audit.record(counselorId, studentId, allowed);
		if (!allowed) {
			throw new StudentAccessDeniedException();
		}
		String name = users.findById(studentId).map(UserSummary::displayName).orElse("");
		return new Detail(studentId, name, reflections.reflectionsOf(studentId), feed.abandonedCareers(studentId),
				feed.topAffinities(studentId, TOP_AFFINITIES), intake.findIntake(studentId).orElse(null));
	}

	/**
	 * Actively linked students, most recently active first (never-active students last, by id). The ordering needs
	 * every student's activity, so the whole (small, per-counselor) set is summarised and then sliced. A deactivated
	 * counselor sees nothing, like {@link CounselorService#canSee}.
	 */
	public Page<Row> students(long counselorId, Pageable pageable) {
		boolean active = users.findById(counselorId).map(UserSummary::active).orElse(false);
		List<Long> ids = active ? links.findActiveStudentIds(counselorId) : List.of();
		Map<Long, ViewActivity> views = careers.viewActivityOf(ids);
		Map<Long, ReflectionActivity> refl = reflections.activityOf(ids);
		Map<Long, AbandonActivity> abandons = feed.abandonActivityOf(ids);
		Map<Long, Instant> intakes = intake.lastIntakeActivityOf(ids);

		Map<Long, UserSummary> students = users.findByIds(ids);
		List<Row> sorted = ids.stream().map(id -> {
			ViewActivity v = views.get(id);
			ReflectionActivity r = refl.get(id);
			AbandonActivity a = abandons.get(id);
			Instant last = Stream.of(v == null ? null : v.lastViewedAt(), r == null ? null : r.lastActivity(),
					a == null ? null : a.lastAbandonedAt(), intakes.get(id)).filter(Objects::nonNull)
					.max(Comparator.naturalOrder()).orElse(null);
			String name = Optional.ofNullable(students.get(id)).map(UserSummary::displayName).orElse("");
			return new Row(id, name, v == null ? 0 : v.careersSeen(), r == null ? 0 : r.reflected(),
					a == null ? 0 : a.abandonedCareers(), r == null ? 0 : r.yes(), r == null ? 0 : r.maybe(),
					r == null ? 0 : r.no(), intakes.containsKey(id), last);
		}).sorted(Comparator.comparing(Row::lastActive, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(Row::studentId)).toList();

		int from = (int) Math.min(pageable.getOffset(), sorted.size());
		int to = Math.min(from + pageable.getPageSize(), sorted.size());
		return new PageImpl<>(sorted.subList(from, to), pageable, sorted.size());
	}

}
