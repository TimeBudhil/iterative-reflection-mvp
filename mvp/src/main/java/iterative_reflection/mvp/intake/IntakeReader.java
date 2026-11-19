package iterative_reflection.mvp.intake;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Read-only public API of the intake module (counselor dashboard, admin pilot metrics). */
public interface IntakeReader {

	/** The student's current intake answers, or empty if never submitted (a dismissal is not an intake). */
	Optional<IntakeView> findIntake(long studentId);

	boolean hasCompletedIntake(long studentId);

	/** When each student last submitted or edited their intake; students without an intake are absent. */
	java.util.Map<Long, java.time.Instant> lastIntakeActivityOf(Collection<Long> studentIds);

	/** Subset of {@code studentIds} that have submitted an intake (one query; for lists and metrics). */
	Set<Long> studentsWithIntake(Collection<Long> studentIds);

}
