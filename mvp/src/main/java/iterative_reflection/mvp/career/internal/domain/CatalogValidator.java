package iterative_reflection.mvp.career.internal.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

/**
 * Validates the whole stored catalog (R3.1, R3.3): summary length, duration, tags, contiguous pages, typed blocks and
 * URL hosts. Pure over a {@link JdbcOperations}, so it runs against any migrated schema; the Flyway callback calls it
 * after every migrate, which covers every seed migration.
 */
@Component
public class CatalogValidator {

	private final Set<String> embedHosts;

	CatalogValidator(@Value("${career.embed-hosts}") List<String> embedHosts) {
		this.embedHosts = embedHosts.stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
	}

	/** @return human-readable violations, empty when the catalog is valid */
	public List<String> validate(JdbcOperations jdbc) {
		List<String> problems = new ArrayList<>();
		List<long[]> ids = new ArrayList<>();
		List<String[]> careers = new ArrayList<>();
		jdbc.query("select id, title, summary, estimated_minutes from careers order by id", rs -> {
			ids.add(new long[] { rs.getLong("id"), rs.getInt("estimated_minutes") });
			careers.add(new String[] { rs.getString("title"), rs.getString("summary") });
		});
		for (int i = 0; i < ids.size(); i++) {
			long id = ids.get(i)[0];
			int minutes = (int) ids.get(i)[1];
			String where = "Career '" + careers.get(i)[0] + "'";
			int sentences = countSentences(careers.get(i)[1]);
			if (sentences < 3 || sentences > 5) {
				problems.add(where + ": summary must be 3-5 sentences but has " + sentences);
			}
			if (minutes < 5 || minutes > 10) {
				problems.add(where + ": estimated duration must be 5-10 minutes but is " + minutes);
			}
			Integer tags = jdbc.queryForObject("select count(*) from career_tags where career_id = ?", Integer.class, id);
			if (tags == null || tags < 1) {
				problems.add(where + ": needs at least one tag");
			}
			List<Integer> pages = jdbc.queryForList(
					"select page_number from career_pages where career_id = ? order by page_number", Integer.class, id);
			if (pages.isEmpty()) {
				problems.add(where + ": needs at least one page");
			}
			for (int p = 0; p < pages.size(); p++) {
				if (pages.get(p) != p + 1) {
					problems.add(where + ": page numbers must run 1.." + pages.size() + " without gaps");
					break;
				}
			}
		}
		jdbc.query("""
				select c.title, p.page_number, b.block_order, b.block_type, b.body, b.url
				from content_blocks b join career_pages p on p.id = b.page_id join careers c on c.id = p.career_id
				order by c.id, p.page_number, b.block_order
				""", rs -> {
			String where = "Career '" + rs.getString("title") + "' page " + rs.getInt("page_number") + " block "
					+ rs.getInt("block_order");
			String type = rs.getString("block_type");
			String body = rs.getString("body");
			String url = rs.getString("url");
			if (type.equals("TEXT")) {
				if (body == null || body.isBlank() || url != null) {
					problems.add(where + ": TEXT block needs a body and no url");
				}
				return;
			}
			if (url == null || body != null) {
				problems.add(where + ": " + type + " block needs a url and no body");
				return;
			}
			String error = urlError(type, url);
			if (error != null) {
				problems.add(where + ": " + error);
			}
		});
		return problems;
	}

	private String urlError(String type, String url) {
		URI uri;
		try {
			uri = new URI(url);
		}
		catch (URISyntaxException e) {
			return "malformed url '" + url + "'";
		}
		String host = uri.getHost();
		if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) {
			return "url must be an https url without credentials: '" + url + "'";
		}
		boolean embed = type.equals("VIDEO_EMBED") || type.equals("AUDIO_EMBED");
		if (embed && !embedHosts.contains(host.toLowerCase(Locale.ROOT))) {
			return "embed host '" + host + "' is not on the allowlist";
		}
		return null;
	}

	static int countSentences(String summary) {
		if (summary == null || summary.isBlank()) {
			return 0;
		}
		int n = 0;
		for (String s : summary.trim().split("(?<=[.!?])\\s+")) {
			if (!s.isBlank()) {
				n++;
			}
		}
		return n;
	}

}
