package iterative_reflection.mvp.counselor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public API of the counselor module. */
public interface CounselorService {

	/** Creates the profile for an existing COUNSELOR user with a unique join code. Joins the caller's transaction. */
	CounselorProfile createProfile(long userId);

	Optional<CounselorProfile> findByUserId(long userId);

	/**
	 * Links the student to the counselor owning {@code code} (case-insensitive, trimmed), or reactivates a revoked
	 * link. Throws a 404 problem for an unknown code or a deactivated counselor and a 409 problem if already
	 * linked. Joins the caller's transaction.
	 */
	CounselorLink link(long studentId, String code);

	/** Revokes the student's own active link; a 404 problem if there is none with that id. */
	void revoke(long studentId, long linkId);

	/** The student's active (not revoked) links, including those to deactivated counselors. */
	Page<CounselorLink> linksOf(long studentId, Pageable pageable);

	/**
	 * Whether the counselor may see the student: an active link exists and the counselor account is active.
	 * Tickets 14+ must call this before returning any student data to a counselor.
	 */
	boolean canSee(long counselorId, long studentId);

	/** Replaces the counselor's join code; existing links are unaffected. Returns the new profile. */
	CounselorProfile rotateJoinCode(long counselorId);

	/** Join code and active linked-student count for each of the given counselor ids that has a profile. */
	Map<Long, CounselorOverview> overviews(Collection<Long> counselorIds);

}
