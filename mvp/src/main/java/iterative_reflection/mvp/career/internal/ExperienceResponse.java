package iterative_reflection.mvp.career.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.career.TagView;
import java.util.List;

record ExperienceResponse(long id, String title, String summary, int estimatedMinutes, List<TagView> tags,
		int lastPage, int lastViewedPage, List<Page> pages) {

	static ExperienceResponse of(CareerSummary c, int lastViewedPage, List<Page> pages) {
		return new ExperienceResponse(c.id(), c.title(), c.summary(), c.estimatedMinutes(), c.tags(), c.lastPage(),
				lastViewedPage, pages);
	}

	record Page(int number, String title, List<Block> blocks) {
	}

	/** {@code text} is set for TEXT blocks; {@code url} (and optional {@code label}) for all other types. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Block(String type, String text, String url, String label) {
	}

}
