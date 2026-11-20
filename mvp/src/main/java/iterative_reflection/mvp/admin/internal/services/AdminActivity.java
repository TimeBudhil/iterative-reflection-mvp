package iterative_reflection.mvp.admin.internal.services;

import iterative_reflection.mvp.admin.internal.repository.AdminActivityWriter;
import java.time.Clock;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Best-effort admin activity log (B-R11.6). A failed write is logged at WARN (the MDC request ID rides on every log
 * line and is also named in the message) and never reaches the caller, so it can't fail or roll back the action.
 * This deliberately differs from the same-transaction listener rule used by {@code ReflectionSaved}/{@code IntakeSaved}.
 */
@Service
public class AdminActivity {

	/** The kinds of admin action the log records; the name is what is stored and returned by the API. */
	public enum Action {

		THEME_CHANGED, COUNSELOR_CREATED, COUNSELOR_DEACTIVATED, COUNSELOR_REACTIVATED

	}

	private static final Logger log = LoggerFactory.getLogger(AdminActivity.class);

	private final AdminActivityWriter writer;

	private final JsonMapper json;

	private final Clock clock;

	AdminActivity(AdminActivityWriter writer, JsonMapper json, Clock clock) {
		this.writer = writer;
		this.json = json;
		this.clock = clock;
	}

	/** Writes now, in its own transaction. */
	public void record(long adminId, Action action, Map<String, String> detail) {
		try {
			writer.insert(adminId, action.name(), json.writeValueAsString(detail), clock.instant());
		}
		catch (RuntimeException e) {
			log.warn("Admin activity write failed (action={}, requestId={}); the action itself is unaffected", action,
					MDC.get("requestId"), e);
		}
	}

	/** Writes after the surrounding transaction commits, so a rolled-back action leaves no row. */
	public void recordAfterCommit(long adminId, Action action, Map<String, String> detail) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			record(adminId, action, detail);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				record(adminId, action, detail);
			}
		});
	}

}
