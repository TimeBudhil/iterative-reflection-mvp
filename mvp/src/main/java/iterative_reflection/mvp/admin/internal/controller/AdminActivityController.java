package iterative_reflection.mvp.admin.internal.controller;

import iterative_reflection.mvp.admin.internal.dto.ActivityEntry;
import iterative_reflection.mvp.admin.internal.services.AdminActivityQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The admin activity log [B-R11.8]: newest first, paginated per N8.1. */
@RestController
public class AdminActivityController {

	private final AdminActivityQuery query;

	AdminActivityController(AdminActivityQuery query) {
		this.query = query;
	}

	@GetMapping("/api/admin/activity")
	Page<ActivityEntry> activity(Pageable pageable) {
		return query.page(pageable);
	}

}
