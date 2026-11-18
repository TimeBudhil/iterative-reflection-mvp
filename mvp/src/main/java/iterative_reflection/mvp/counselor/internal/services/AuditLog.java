package iterative_reflection.mvp.counselor.internal.services;

import iterative_reflection.mvp.counselor.AuditEntry;
import iterative_reflection.mvp.counselor.internal.repository.AuditRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Append-only record of counselor detail reads [R8.6]. */
@Service
public class AuditLog {

	private final AuditRepository repository;

	private final Clock clock;

	AuditLog(AuditRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	/** Own transaction: a denied request fails with 403 but its row must survive any enclosing rollback. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void record(long counselorId, long studentId, boolean allowed) {
		repository.insert(counselorId, studentId, allowed ? "ALLOWED" : "DENIED", clock.instant());
	}

	/** Newest first; all students when {@code studentId} is null. */
	@Transactional(readOnly = true)
	Page<AuditEntry> find(Long studentId, Pageable pageable) {
		List<AuditEntry> content = repository.find(studentId, pageable.getPageSize(), pageable.getOffset());
		return new PageImpl<>(content, pageable, repository.count(studentId));
	}

}
