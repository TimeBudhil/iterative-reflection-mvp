package iterative_reflection.mvp.career.internal.domain;

import java.util.List;

/** The stored catalog as plain records, so validation needs no database. */
public record CatalogSnapshot(List<CareerRow> careers, List<BlockRow> blocks) {

	public record CareerRow(long id, String title, String summary, int estimatedMinutes, int tagCount,
			List<Integer> pageNumbers) {
	}

	public record BlockRow(String careerTitle, int pageNumber, int blockOrder, String type, String body, String url) {
	}

}
