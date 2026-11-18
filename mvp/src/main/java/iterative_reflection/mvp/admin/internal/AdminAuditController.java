package iterative_reflection.mvp.admin.internal;

import iterative_reflection.mvp.counselor.AuditEntry;
import iterative_reflection.mvp.counselor.CounselorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminAuditController {

	private final CounselorService counselors;

	AdminAuditController(CounselorService counselors) {
		this.counselors = counselors;
	}

	/** Counselor detail reads, newest first; optionally for one student [R9.6]. */
	@GetMapping("/api/admin/audit")
	Page<AuditEntry> audit(@RequestParam(required = false) Long studentId, Pageable pageable) {
		return counselors.auditLog(studentId, pageable);
	}

}
