package iterative_reflection.mvp.admin.internal.services;

import iterative_reflection.mvp.admin.internal.dto.ActivityEntry;
import iterative_reflection.mvp.admin.internal.repository.AdminActivityReader;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Reads the admin activity log, newest first, with admin display names from {@code identity}. */
@Service
public class AdminActivityQuery {

	private final AdminActivityReader reader;

	private final UserService users;

	private final JsonMapper json;

	AdminActivityQuery(AdminActivityReader reader, UserService users, JsonMapper json) {
		this.reader = reader;
		this.users = users;
		this.json = json;
	}

	@Transactional(readOnly = true)
	public Page<ActivityEntry> page(Pageable pageable) {
		List<AdminActivityReader.Row> rows = reader.find(pageable);
		Set<Long> ids = rows.stream().map(AdminActivityReader.Row::adminId).collect(Collectors.toSet());
		Map<Long, UserSummary> admins = users.findByIds(ids);
		List<ActivityEntry> content = rows.stream().map(r -> new ActivityEntry(r.createdAt(),
				admins.containsKey(r.adminId()) ? admins.get(r.adminId()).displayName() : null, r.action(),
				json.readValue(r.detailJson(), new TypeReference<Map<String, String>>() {
				}))).toList();
		return new PageImpl<>(content, pageable, reader.count());
	}

}
