package iterative_reflection.mvp.career;

import java.util.List;

/** Card-level view of a career. {@code lastPage} is the number of the final page (pages are 1-based). */
public record CareerSummary(long id, String title, String summary, int estimatedMinutes, boolean active,
		int lastPage, List<TagView> tags) {
}
