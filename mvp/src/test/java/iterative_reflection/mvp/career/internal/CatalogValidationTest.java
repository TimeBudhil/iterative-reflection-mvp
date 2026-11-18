package iterative_reflection.mvp.career.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import java.util.function.Consumer;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class CatalogValidationTest extends HttpIntegrationTest {

	@Autowired
	CatalogValidator validator;

	@Autowired
	TransactionTemplate tx;

	/** Runs {@code arrange} against the live schema, validates, and rolls everything back. */
	List<String> violationsAfter(Consumer<JdbcTemplate> arrange) {
		return tx.execute(status -> {
			status.setRollbackOnly();
			arrange.accept(jdbc);
			return validator.validate(jdbc);
		});
	}

	void career(JdbcTemplate j, String title, String summary, int minutes) {
		j.update("insert into careers (title, summary, estimated_minutes, default_order, active) values (?,?,?,1,true)",
				title, summary, minutes);
		j.update("insert into career_tags (career_id, tag_id) select c.id, t.id from careers c, tags t "
				+ "where c.title = ? and t.name = 'Fixture health'", title);
		j.update("insert into career_pages (career_id, page_number) select id, 1 from careers where title = ?", title);
		j.update("insert into content_blocks (page_id, block_order, block_type, body) "
				+ "select p.id, 1, 'TEXT', 'hi' from career_pages p join careers c on c.id = p.career_id where c.title = ?",
				title);
	}

	@Test
	void fixtureCatalogIsValid() {
		assertThat(validator.validate(jdbc)).isEmpty();
	}

	@Test
	void summaryMustBeThreeToFiveSentences() {
		assertThat(violationsAfter(j -> career(j, "Short", "One. Two.", 6)))
				.anyMatch(v -> v.contains("Short") && v.contains("sentence"));
		assertThat(violationsAfter(j -> career(j, "Long", "A. B. C. D. E. F.", 6)))
				.anyMatch(v -> v.contains("Long") && v.contains("sentence"));
		assertThat(violationsAfter(j -> career(j, "Fine", "A. B! C?", 6))).isEmpty();
	}

	@Test
	void durationMustBeFiveToTenMinutes() {
		assertThat(violationsAfter(j -> career(j, "Quick", "A. B. C.", 4)))
				.anyMatch(v -> v.contains("Quick") && v.contains("minutes"));
		assertThat(violationsAfter(j -> career(j, "Slow", "A. B. C.", 11)))
				.anyMatch(v -> v.contains("Slow") && v.contains("minutes"));
	}

	@Test
	void blockUrlsAreValidated() {
		for (String bad : List.of("https://evil.example.com/x", "http://www.youtube.com/embed/1",
				"https://www.youtube.com@evil.example.com/x", "not a url", "https://youtube.com.evil.com/x")) {
			var v = violationsAfter(j -> {
				career(j, "Embed", "A. B. C.", 6);
				j.update("insert into content_blocks (page_id, block_order, block_type, url) "
						+ "select p.id, 2, 'VIDEO_EMBED', ? from career_pages p join careers c on c.id = p.career_id "
						+ "where c.title = 'Embed'", bad);
			});
			assertThat(v).as(bad).anyMatch(s -> s.contains("Embed"));
		}
	}

	@Test
	void pagesMustBeContiguousAndTagsRequired() {
		var v = violationsAfter(j -> {
			career(j, "Gappy", "A. B. C.", 6);
			j.update("insert into career_pages (career_id, page_number) select id, 3 from careers where title = 'Gappy'");
		});
		assertThat(v).anyMatch(s -> s.contains("Gappy") && s.contains("page"));
		var noTags = violationsAfter(j -> {
			career(j, "Untagged", "A. B. C.", 6);
			j.update("delete from career_tags where career_id = (select id from careers where title = 'Untagged')");
		});
		assertThat(noTags).anyMatch(s -> s.contains("Untagged") && s.contains("tag"));
	}

	@Test
	void tagsMustBelongToTheTaxonomy() {
		assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
			status.setRollbackOnly();
			career(jdbc, "Tagged", "A. B. C.", 6);
			jdbc.update(
					"insert into career_tags (career_id, tag_id) select id, 424242 from careers where title = 'Tagged'");
		})).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void aSeedMigrationWithANonAllowlistedEmbedIsRefusedAtMigrateTime() {
		Flyway flyway = Flyway.configure()
				.dataSource("jdbc:h2:mem:badseed;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "")
				.locations("classpath:db/migration", "classpath:db/fixtures", "classpath:db/bad-career")
				.callbacks(new CatalogValidationCallback(validator))
				.load();
		assertThatThrownBy(flyway::migrate).isInstanceOf(FlywayException.class)
				.hasMessageContaining("evil.example.com");
	}

}
