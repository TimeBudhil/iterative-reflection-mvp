package iterative_reflection.mvp.career.internal.repository;

import iterative_reflection.mvp.career.internal.domain.CareerPage;
import iterative_reflection.mvp.career.internal.domain.ContentBlock;
import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.career.TagView;
import iterative_reflection.mvp.career.ViewActivity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JdbcCareerCatalog implements CareerCatalog {

	private static final String CAREER_SQL = """
			select c.id, c.title, c.summary, c.estimated_minutes, c.active, c.default_order,
			  coalesce((select max(p.page_number) from career_pages p where p.career_id = c.id), 0) as last_page
			from careers c
			""";

	private final JdbcClient jdbc;

	private final Clock clock;

	JdbcCareerCatalog(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Override
	public Optional<CareerSummary> findCareer(long careerId) {
		return jdbc.sql(CAREER_SQL + " where c.id = :id").param("id", careerId)
				.query((rs, n) -> row(rs)).optional().map(r -> r.withTags(tagsFor(List.of(careerId)).get(careerId)));
	}

	@Override
	public int lastPage(long careerId) {
		return jdbc.sql("select coalesce(max(page_number), 0) from career_pages where career_id = :id")
				.param("id", careerId).query(Integer.class).single();
	}

	@Override
	public int lastViewedPage(long studentId, long careerId) {
		int stored = jdbc.sql("select last_viewed_page from page_progress where student_id = :s and career_id = :c")
				.param("s", studentId).param("c", careerId).query(Integer.class).optional().orElse(0);
		return Math.min(stored, lastPage(careerId));
	}

	@Override
	public List<CareerSummary> activeCareersInDefaultOrder() {
		List<Row> rows = jdbc.sql(CAREER_SQL + " where c.active = true order by c.default_order, c.id")
				.query((rs, n) -> row(rs)).list();
		Map<Long, List<TagView>> tags = tagsFor(rows.stream().map(Row::id).toList());
		return rows.stream().map(r -> r.withTags(tags.get(r.id()))).toList();
	}

	@Override
	public Map<Long, ViewActivity> viewActivityOf(java.util.Collection<Long> studentIds) {
		Map<Long, ViewActivity> result = new java.util.HashMap<>();
		if (studentIds.isEmpty()) {
			return result;
		}
		jdbc.sql("""
				select student_id, count(*) as seen, max(updated_at) as last_viewed
				from page_progress where student_id in (:ids) and last_viewed_page >= 1 group by student_id
				""").param("ids", studentIds).query((rs, n) -> {
					result.put(rs.getLong("student_id"), new ViewActivity(rs.getInt("seen"),
							rs.getTimestamp("last_viewed").toInstant()));
					return 0;
				}).list();
		return result;
	}

	@Override
	public List<TagView> allTags() {
		return jdbc.sql("""
				select t.id, t.name, tc.name as category from tags t join tag_categories tc on tc.id = t.category_id
				order by tc.display_order, t.name""")
				.query((rs, n) -> new TagView(rs.getLong("id"), rs.getString("category"), rs.getString("name")))
				.list();
	}

	/** Records that the student viewed {@code page}; progress never moves backwards. Returns the new progress. */
	@Transactional
	public int recordViewed(long studentId, long careerId, int page) {
		Instant now = clock.instant();
		int updated = update(studentId, careerId, page, now);
		if (updated == 0) {
			try {
				jdbc.sql("insert into page_progress (student_id, career_id, last_viewed_page, updated_at) "
						+ "values (:s, :c, :p, :t)").param("s", studentId).param("c", careerId).param("p", page)
						.param("t", Timestamp.from(now)).update();
			}
			catch (DuplicateKeyException e) {
				update(studentId, careerId, page, now);
			}
		}
		return lastViewedPage(studentId, careerId);
	}

	private int update(long studentId, long careerId, int page, Instant now) {
		return jdbc.sql("update page_progress set last_viewed_page = :p, updated_at = :t "
				+ "where student_id = :s and career_id = :c and last_viewed_page < :p").param("s", studentId)
				.param("c", careerId).param("p", page).param("t", Timestamp.from(now)).update();
	}

	/** Experience content: pages with blocks, in order. */
	public List<CareerPage> pages(long careerId) {
		Map<Long, CareerPage> pages = new LinkedHashMap<>();
		jdbc.sql("select id, page_number, title from career_pages where career_id = :c order by page_number")
				.param("c", careerId).query((rs, n) -> {
					pages.put(rs.getLong("id"), new CareerPage(rs.getInt("page_number"),
							rs.getString("title"), new ArrayList<>()));
					return 0;
				}).list();
		jdbc.sql("select b.page_id, b.block_type, b.body, b.url, b.label from content_blocks b "
				+ "join career_pages p on p.id = b.page_id where p.career_id = :c order by p.page_number, b.block_order")
				.param("c", careerId).query((rs, n) -> {
					pages.get(rs.getLong("page_id")).blocks().add(new ContentBlock(
							rs.getString("block_type"), rs.getString("body"), rs.getString("url"),
							rs.getString("label")));
					return 0;
				}).list();
		return new ArrayList<>(pages.values());
	}

	private Map<Long, List<TagView>> tagsFor(List<Long> careerIds) {
		Map<Long, List<TagView>> result = new LinkedHashMap<>();
		careerIds.forEach(id -> result.put(id, new ArrayList<>()));
		if (careerIds.isEmpty()) {
			return result;
		}
		jdbc.sql("""
				select ct.career_id, t.id, t.name, tc.name as category
				from career_tags ct join tags t on t.id = ct.tag_id join tag_categories tc on tc.id = t.category_id
				where ct.career_id in (:ids) order by tc.display_order, t.name
				""").param("ids", careerIds).query((rs, n) -> {
					result.get(rs.getLong("career_id"))
							.add(new TagView(rs.getLong("id"), rs.getString("category"), rs.getString("name")));
					return 0;
				}).list();
		return result;
	}

	private Row row(ResultSet rs) throws SQLException {
		return new Row(rs.getLong("id"), rs.getString("title"), rs.getString("summary"),
				rs.getInt("estimated_minutes"), rs.getBoolean("active"), rs.getInt("last_page"));
	}

	private record Row(long id, String title, String summary, int minutes, boolean active, int lastPage) {

		CareerSummary withTags(List<TagView> tags) {
			return new CareerSummary(id, title, summary, minutes, active, lastPage, tags);
		}

	}

}
