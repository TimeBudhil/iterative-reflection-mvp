package iterative_reflection.mvp.admin.internal.services;

import iterative_reflection.mvp.admin.internal.dto.ActivityEntry;
import iterative_reflection.mvp.admin.internal.repository.AdminActivityReader;
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

	private final AdminNames names;

	private final JsonMapper json;

	AdminActivityQuery(AdminActivityReader reader, AdminNames names, JsonMapper json) {
		this.reader = reader;
		this.names = names;
		this.json = json;
	}

	@Transactional(readOnly = true)
	public Page<ActivityEntry> page(Pageable pageable) {
		List<AdminActivityReader.Row> rows = reader.find(pageable);
		Set<Long> ids = rows.stream().map(AdminActivityReader.Row::adminId).collect(Collectors.toSet());
		Map<Long, String> admins = names.namesOf(ids);
		List<ActivityEntry> content = rows.stream().map(r -> new ActivityEntry(r.id(), r.createdAt(),
				admins.get(r.adminId()), r.action(),
				json.readValue(r.detailJson(), new TypeReference<Map<String, String>>() {
				}))).toList();
		return new PageImpl<>(content, pageable, reader.count());
	}

}
