package iterative_reflection.mvp.career.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** R10.2 / R10.3: the real seed catalog (fixtures sit at default_order 9000+, seed careers below). */
class SeedContentTest extends HttpIntegrationTest {

	static final String SEED = "c.default_order < 9000";

	@Autowired
	Flyway flyway;

	@Autowired
	CatalogValidator validator;

	int count(String sql) {
		Integer n = jdbc.queryForObject(sql, Integer.class);
		return n == null ? 0 : n;
	}

	@Test
	void seedCatalogCoversTwentyCareersAcrossFiveClusters() {
		assertThat(count("select count(*) from careers c where " + SEED)).isGreaterThanOrEqualTo(20);
		assertThat(count("""
				select count(distinct t.id) from career_tags ct join careers c on c.id = ct.career_id
				join tags t on t.id = ct.tag_id join tag_categories g on g.id = t.category_id
				where g.name = 'Cluster' and %s""".formatted(SEED))).isGreaterThanOrEqualTo(5);
	}

	@Test
	void everySeedCareerHasThreePagesAnAllowlistedEmbedAndTwoTags() {
		List<String> titles = jdbc.queryForList("select c.title from careers c where " + SEED, String.class);
		assertThat(titles).isNotEmpty();
		for (String title : titles) {
			assertThat(count("select count(*) from career_pages p join careers c on c.id = p.career_id where c.title = '"
					+ title.replace("'", "''") + "'")).as(title + " pages").isGreaterThanOrEqualTo(3);
			assertThat(count("""
					select count(*) from content_blocks b join career_pages p on p.id = b.page_id
					join careers c on c.id = p.career_id
					where b.block_type in ('VIDEO_EMBED','AUDIO_EMBED') and c.title = '%s'"""
					.formatted(title.replace("'", "''")))).as(title + " embeds").isGreaterThanOrEqualTo(1);
			assertThat(count("select count(*) from career_tags ct join careers c on c.id = ct.career_id "
					+ "where c.title = '" + title.replace("'", "''") + "'")).as(title + " tags").isGreaterThanOrEqualTo(2);
		}
		assertThat(validator.validate(jdbc)).isEmpty();
	}

	@Test
	void everyClusterHasThreeCareersIncludingTradeSchool() {
		assertThat(jdbc.queryForList("select name from tags where category_id = "
				+ "(select id from tag_categories where name = 'Cluster')", String.class))
				.contains("Skilled Trades (Trade School)").hasSizeGreaterThanOrEqualTo(9);
		assertThat(count("""
				select count(*) from career_tags ct join tags t on t.id = ct.tag_id
				where t.name = 'Skilled Trades (Trade School)'""")).isEqualTo(3);
	}

	@Test
	void migratingAgainDoesNotDuplicateSeedRows() {
		int careers = count("select count(*) from careers");
		int blocks = count("select count(*) from content_blocks");
		int tags = count("select count(*) from tags");
		flyway.migrate();
		assertThat(count("select count(*) from careers")).isEqualTo(careers);
		assertThat(count("select count(*) from content_blocks")).isEqualTo(blocks);
		assertThat(count("select count(*) from tags")).isEqualTo(tags);
		assertThat(count("select count(*) from (select title from careers group by title having count(*) > 1) d"))
				.isZero();
	}

}
