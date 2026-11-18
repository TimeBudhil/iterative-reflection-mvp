package iterative_reflection.mvp.counselor;

import java.util.Optional;

/** Public API of the counselor module. */
public interface CounselorService {

	/** Creates the profile for an existing COUNSELOR user with a unique join code. Joins the caller's transaction. */
	CounselorProfile createProfile(long userId);

	Optional<CounselorProfile> findByUserId(long userId);

}
