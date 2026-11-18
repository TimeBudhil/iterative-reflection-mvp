package iterative_reflection.mvp.identity;

import java.util.Optional;

/** Public API of the identity module. Later tickets add: create counselor, set active/inactive. */
public interface UserService {

	Optional<UserSummary> findById(long id);

}
