package iterative_reflection.mvp.admin.internal.services;

import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Admin id to display name, from {@code identity}; a name is absent when the admin no longer exists. */
@Service
public class AdminNames {

	private final UserService users;

	AdminNames(UserService users) {
		this.users = users;
	}

	/** The display name, or null for a null id or an admin that no longer exists. */
	public String nameOf(Long adminId) {
		return adminId == null ? null : users.findById(adminId).map(UserSummary::displayName).orElse(null);
	}

	/** Names for all ids in one lookup; ids of vanished admins are absent from the result. */
	public Map<Long, String> namesOf(Collection<Long> adminIds) {
		return users.findByIds(Set.copyOf(adminIds)).entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().displayName()));
	}

}
