package iterative_reflection.mvp.career.internal.dto;

import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.career.TagView;
import iterative_reflection.mvp.career.internal.domain.CareerPage;
import java.util.List;

public record ExperienceResponse(long id, String title, String summary, int estimatedMinutes, List<TagView> tags,
		int lastPage, int lastViewedPage, List<CareerPage> pages) {

	public static ExperienceResponse of(CareerSummary c, int lastViewedPage, List<CareerPage> pages) {
		return new ExperienceResponse(c.id(), c.title(), c.summary(), c.estimatedMinutes(), c.tags(), c.lastPage(),
				lastViewedPage, pages);
	}

}
