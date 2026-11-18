package iterative_reflection.mvp.identity;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Public API of the identity module.  */
public interface UserService {

	Optional<UserSummary> findById(long id);

	/** One query for many users; unknown ids are simply absent from the result. */
	java.util.Map<Long, UserSummary> findByIds(java.util.Collection<Long> ids);

	/** Case-insensitive lookup. */
	Optional<UserSummary> findByEmail(String email);

	/**
	 * Dev seed only: creates an active account flagged {@code demo} with no password-length rule and no forced
	 * password change. Caller must check {@link #findByEmail} first (idempotence is the caller's job).
	 */
	UserSummary createDemoAccount(String email, String displayName, String password, Role role);

	/**
	 * Creates an active COUNSELOR with a temporary password and {@code mustChangePassword = true}. Throws a 409
	 * problem if the email exists (any case) and a 400 problem if the password breaks the length rules.
	 */
	UserSummary createCounselor(String email, String displayName, String temporaryPassword);

	/** Users with the role, newest first (id descending). */
	Page<UserSummary> findByRole(Role role, Pageable pageable);

	/**
	 * Activates or deactivates the user. Deactivating also deletes all of the user's sessions from the store, so
	 * their next request gets 401. Throws a 404 problem if the user does not exist.
	 */
	UserSummary setActive(long userId, boolean active);

	/**
	 * Pilot cohort size: active, non-demo STUDENT accounts created in {@code [from, to)}; a null bound is open.
	 * One aggregate query.
	 */
	long countPilotStudents(java.time.Instant from, java.time.Instant to);

}
