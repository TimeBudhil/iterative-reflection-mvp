package iterative_reflection.mvp.career.internal.repository;

import iterative_reflection.mvp.career.internal.domain.CatalogSnapshot;
import iterative_reflection.mvp.career.internal.domain.CatalogSnapshot.BlockRow;
import iterative_reflection.mvp.career.internal.domain.CatalogSnapshot.CareerRow;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** Reads the whole stored catalog over the given connection (the Flyway callback has no other one at migrate time). */
@Repository
public class CatalogSnapshotRepository {

	public CatalogSnapshot load(JdbcOperations jdbc) {
		List<CareerRow> careers = new ArrayList<>();
		jdbc.query("select id, title, summary, estimated_minutes from careers order by id", rs -> {
			long id = rs.getLong("id");
			int tags = jdbc.queryForObject("select count(*) from career_tags where career_id = ?", Integer.class, id);
			List<Integer> pages = jdbc.queryForList(
					"select page_number from career_pages where career_id = ? order by page_number", Integer.class, id);
			careers.add(new CareerRow(id, rs.getString("title"), rs.getString("summary"),
					rs.getInt("estimated_minutes"), tags, pages));
		});
		List<BlockRow> blocks = new ArrayList<>();
		jdbc.query("""
				select c.title, p.page_number, b.block_order, b.block_type, b.body, b.url
				from content_blocks b join career_pages p on p.id = b.page_id join careers c on c.id = p.career_id
				order by c.id, p.page_number, b.block_order
				""", rs -> {
			blocks.add(new BlockRow(rs.getString("title"), rs.getInt("page_number"), rs.getInt("block_order"),
					rs.getString("block_type"), rs.getString("body"), rs.getString("url")));
		});
		return new CatalogSnapshot(careers, blocks);
	}

}
