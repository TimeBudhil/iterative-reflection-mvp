package iterative_reflection.mvp.career;

import java.util.List;
import java.util.Optional;

/** Public API of the career module (read-only; consumed by feed, reflection and abandon). */
public interface CareerCatalog {

	/** Any career, active or retired. */
	Optional<CareerSummary> findCareer(long careerId);

	/** Number of the last page (pages are numbered 1..n); 0 if the career has no pages or does not exist. */
	int lastPage(long careerId);

	/** The student's highest viewed page, clamped to the career's current last page; 0 if none viewed. */
	int lastViewedPage(long studentId, long careerId);

	/** Active careers ordered by the catalog default order (ties by id). */
	List<CareerSummary> activeCareersInDefaultOrder();

	/** Every taxonomy tag (all categories, clusters included), ordered by category then name. */
	List<TagView> allTags();

}
