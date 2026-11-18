package iterative_reflection.mvp.identity;

import java.util.Optional;

/** Public API of the identity module. Ticket 15 adds set active/inactive. */
public interface UserService {

	Optional<UserSummary> findById(long id);

	/**
	 * Creates an active COUNSELOR with a temporary password and {@code mustChangePassword = true}. Throws a 409
	 * problem if the email exists (any case) and a 400 problem if the password breaks the length rules.
	 */
	UserSummary createCounselor(String email, String displayName, String temporaryPassword);

}
