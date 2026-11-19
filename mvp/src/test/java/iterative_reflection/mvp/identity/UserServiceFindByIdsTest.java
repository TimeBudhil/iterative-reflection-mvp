package iterative_reflection.mvp.identity;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Batch lookup in the public API, used by lists that would otherwise load one user per row. */
class UserServiceFindByIdsTest extends HttpIntegrationTest {

	@Autowired
	UserService users;

	@Test
	void returnsTheKnownUsersKeyedByIdAndSkipsUnknownIds() {
		String a = createUser(uniqueEmail(), "correct horse battery", "STUDENT", false);
		String b = createUser(uniqueEmail(), "correct horse battery", "COUNSELOR", false);
		long idA = users.findByEmail(a).orElseThrow().id();
		long idB = users.findByEmail(b).orElseThrow().id();

		Map<Long, UserSummary> found = users.findByIds(List.of(idA, idB, 999_999_999L));

		assertThat(found).containsOnlyKeys(idA, idB);
		assertThat(found.get(idA).email()).isEqualTo(a);
		assertThat(found.get(idB).role()).isEqualTo(Role.COUNSELOR);
		assertThat(users.findByIds(List.of())).isEmpty();
	}

}
