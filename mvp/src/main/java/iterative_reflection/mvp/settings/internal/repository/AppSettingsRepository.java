package iterative_reflection.mvp.settings.internal.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

	/** The stored row for {@code key}, if any. */
	public Optional<Row> find(String key) {
		return jdbc.sql("select value_json, updated_by, updated_at from app_settings where \"key\" = :k")
				.param("k", key).query((rs, i) -> {
					long by = rs.getLong("updated_by");
					Long updatedBy = rs.wasNull() ? null : by;
					return new Row(rs.getString("value_json"), updatedBy,
							rs.getObject("updated_at", OffsetDateTime.class).toInstant());
				}).optional();
	}

	/** Inserts or replaces the row for {@code key} (update first, insert when nothing matched). */
	public void upsert(String key, String valueJson, long updatedBy, Instant updatedAt) {
		OffsetDateTime at = updatedAt.atOffset(ZoneOffset.UTC);
		int updated = jdbc.sql("update app_settings set value_json = :v, updated_by = :by, updated_at = :at "
				+ "where \"key\" = :k").param("v", valueJson).param("by", updatedBy).param("at", at).param("k", key)
				.update();
		if (updated == 0) {
			jdbc.sql("insert into app_settings (\"key\", value_json, updated_by, updated_at) "
					+ "values (:k, :v, :by, :at)").param("k", key).param("v", valueJson).param("by", updatedBy)
					.param("at", at).update();
		}
	}

	public record Row(String valueJson, Long updatedBy, Instant updatedAt) {
	}

}
