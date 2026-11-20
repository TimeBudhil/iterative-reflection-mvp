package iterative_reflection.mvp.settings.internal.repository;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL over the generic {@code app_settings} table; persistence is internal to the settings module. */
@Repository
public class AppSettingsRepository {

	private final JdbcClient jdbc;

	AppSettingsRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** The raw JSON value stored under {@code key}, if any. */
	public Optional<String> findValueJson(String key) {
		return jdbc.sql("select value_json from app_settings where \"key\" = :k").param("k", key).query(String.class)
				.optional();
	}

}
