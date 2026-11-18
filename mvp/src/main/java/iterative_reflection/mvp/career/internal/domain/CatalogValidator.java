package iterative_reflection.mvp.career.internal.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates the whole stored catalog (R3.1, R3.3): summary length, duration, tags, contiguous pages, typed blocks and
 * URL hosts. Pure over a {@link CatalogSnapshot}; the repository loads the snapshot and the Flyway callback validates it
 * after every migrate, which covers every seed migration.
 */
@Component
public class CatalogValidator {

	private final Set<String> embedHosts;

	CatalogValidator(@Value("${career.embed-hosts}") List<String> embedHosts) {
		this.embedHosts = embedHosts.stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
	}

	/** @return human-readable violations, empty when the catalog is valid */
	public List<String> validate(CatalogSnapshot catalog) {
		List<String> problems = new ArrayList<>();
		for (CatalogSnapshot.CareerRow career : catalog.careers()) {
			String where = "Career '" + career.title() + "'";
			int sentences = countSentences(career.summary());
			if (sentences < 3 || sentences > 5) {
				problems.add(where + ": summary must be 3-5 sentences but has " + sentences);
			}
			if (career.estimatedMinutes() < 5 || career.estimatedMinutes() > 10) {
				problems.add(where + ": estimated duration must be 5-10 minutes but is " + career.estimatedMinutes());
			}
			if (career.tagCount() < 1) {
				problems.add(where + ": needs at least one tag");
			}
			List<Integer> pages = career.pageNumbers();
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
		for (CatalogSnapshot.BlockRow block : catalog.blocks()) {
			String where = "Career '" + block.careerTitle() + "' page " + block.pageNumber() + " block "
					+ block.blockOrder();
			if (block.type().equals("TEXT")) {
				if (block.body() == null || block.body().isBlank() || block.url() != null) {
					problems.add(where + ": TEXT block needs a body and no url");
				}
				continue;
			}
			if (block.url() == null || block.body() != null) {
				problems.add(where + ": " + block.type() + " block needs a url and no body");
				continue;
			}
			String error = urlError(block.type(), block.url());
			if (error != null) {
				problems.add(where + ": " + error);
			}
		}
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
