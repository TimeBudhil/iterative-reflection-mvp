package iterative_reflection.mvp.career.internal.integration;

import iterative_reflection.mvp.career.internal.domain.CatalogValidator;
import java.util.List;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Component;

/**
 * Flyway hook: after every migrate run (including no-op runs at startup) the whole catalog is validated, so any
 * seed migration with a bad summary, duration or non-allowlisted embed makes migration, and so startup, fail.
 * Spring Boot registers {@link Callback} beans with Flyway automatically.
 */
@Component
public class CatalogValidationCallback implements Callback {

	private final CatalogValidator validator;

	public CatalogValidationCallback(CatalogValidator validator) {
		this.validator = validator;
	}

	@Override
	public boolean supports(Event event, Context context) {
		return event == Event.AFTER_MIGRATE;
	}

	@Override
	public boolean canHandleInTransaction(Event event, Context context) {
		return true;
	}

	@Override
	public void handle(Event event, Context context) {
		JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
		List<String> problems = validator.validate(jdbc);
		if (!problems.isEmpty()) {
			throw new IllegalStateException("Career catalog failed validation:\n - " + String.join("\n - ", problems));
		}
	}

	@Override
	public String getCallbackName() {
		return "careerCatalogValidation";
	}

}
