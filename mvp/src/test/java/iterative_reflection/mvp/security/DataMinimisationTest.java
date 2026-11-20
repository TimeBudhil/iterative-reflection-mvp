package iterative_reflection.mvp.security;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Only the fields listed in R1-R9 are persisted [N2.8], and no third-party tracking libraries are on board [N6.2].
 * Adding a column or table means updating this list on purpose.
 */
class DataMinimisationTest extends HttpIntegrationTest {

	/** Framework/infrastructure tables (no application data columns of ours); their existence is asserted too. */
	private static final Set<String> INFRASTRUCTURE_TABLES = Set.of("flyway_schema_history", "event_publication",
			"spring_session", "spring_session_attributes");

	private static final Map<String, Set<String>> EXPECTED = new TreeMap<>(Map.ofEntries(
			// R1: accounts. Only the age-verified timestamp is kept, never the birthdate.
			Map.entry("users", Set.of("id", "email", "password_hash", "display_name", "role", "active",
					"must_change_password", "demo", "age_verified_at", "accepted_terms_version", "accepted_terms_at",
					"created_at", "version")),
			// R2, R8: counselors, links, audit
			Map.entry("counselor_profiles", Set.of("user_id", "join_code", "created_at")),
			Map.entry("counselor_links", Set.of("id", "student_id", "counselor_id", "linked_at", "revoked_at")),
			Map.entry("audit_log", Set.of("id", "counselor_id", "student_id", "outcome", "at")),
			// R3, R4: career catalog and viewing progress
			Map.entry("tag_categories", Set.of("id", "name", "display_order")),
			Map.entry("tags", Set.of("id", "category_id", "name")),
			Map.entry("careers", Set.of("id", "title", "summary", "estimated_minutes", "default_order", "active")),
			Map.entry("career_tags", Set.of("career_id", "tag_id")),
			Map.entry("career_pages", Set.of("id", "career_id", "page_number", "title")),
			Map.entry("content_blocks", Set.of("id", "page_id", "block_order", "block_type", "body", "url", "label")),
			Map.entry("page_progress", Set.of("student_id", "career_id", "last_viewed_page", "updated_at")),
			// R5: reflections
			Map.entry("reflection_questions", Set.of("id", "question_key", "version", "prompt", "open_prompt",
					"display_order", "active")),
			Map.entry("reflections", Set.of("id", "student_id", "career_id", "current_version", "version",
					"created_at", "updated_at")),
			Map.entry("reflection_versions", Set.of("id", "reflection_id", "version_number", "verdict", "created_at")),
			Map.entry("reflection_answers", Set.of("id", "reflection_version_id", "question_id", "score", "open_text")),
			// R6, R7: feed, affinity, abandons
			Map.entry("feed_state", Set.of("student_id", "career_id", "served_at")),
			Map.entry("affinity_contributions", Set.of("id", "student_id", "tag_id", "source_type", "source_id", "delta")),
			Map.entry("abandons", Set.of("id", "student_id", "career_id", "last_viewed_page", "abandon_count",
					"abandoned_at")),
			// R9: intake
			Map.entry("intake_questions", Set.of("id", "question_key", "prompt", "question_type", "display_order",
					"active")),
			Map.entry("intake_responses", Set.of("id", "student_id", "created_at", "updated_at")),
			Map.entry("intake_answers", Set.of("id", "response_id", "question_id", "text_value")),
			Map.entry("intake_answer_tags", Set.of("answer_id", "tag_id")),
			Map.entry("intake_dismissals", Set.of("student_id", "dismissed_at")),
			// update 2: app-wide settings (a preset id only; no personal data)
			Map.entry("app_settings", Set.of("key", "value_json", "updated_by", "updated_at"))));

	@Test
	void persistedColumnsAreExactlyTheDocumentedOnes() {
		Map<String, Set<String>> actual = new TreeMap<>();
		jdbc.query("""
				select lower(table_name) t, lower(column_name) c from information_schema.columns
				where lower(table_schema) = 'public'
				""", rs -> {
			actual.computeIfAbsent(rs.getString("t"), k -> new TreeSet<>()).add(rs.getString("c"));
		});
		assertThat(actual.keySet()).containsAll(INFRASTRUCTURE_TABLES);
		INFRASTRUCTURE_TABLES.forEach(actual::remove);
		assertThat(actual).isEqualTo(EXPECTED);
	}

	@Test
	void noTrackingOrAnalyticsDependencies() throws Exception {
		String pom = Files.readString(Path.of("pom.xml")).toLowerCase(Locale.ROOT);
		List<String> banned = List.of("analytics", "segment", "mixpanel", "amplitude", "hotjar", "sentry", "newrelic",
				"datadog", "firebase", "facebook", "matomo", "posthog", "adjust", "appsflyer", "crashlytics",
				"bugsnag", "rollbar", "fullstory", "heap", "intercom");
		for (String word : banned) {
			assertThat(pom).as("pom.xml must not mention '%s'", word).doesNotContainPattern("<(group|artifact)id>[^<]*"
					+ word + "[^<]*<");
		}
	}

}
