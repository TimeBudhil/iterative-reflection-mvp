package iterative_reflection.mvp.feed;

import java.util.List;
import java.util.OptionalLong;

/** Public API of the feed module. */
public interface FeedService {

	/**
	 * Returns the stored in-progress card if it is still valid (career active and not reflected on); otherwise picks
	 * the next career, stores it as in progress and returns it; or an exhausted card when nothing is left [R4.1-R4.6].
	 */
	FeedCard next(long studentId);

	/** The student's valid in-progress career, without serving anything new. */
	OptionalLong inProgressCareerId(long studentId);

	/**
	 * The student's top {@code n} tags by summed affinity, highest first (ties by tag id). Only tags with at least
	 * one contribution appear (scores may be negative).
	 */
	List<TagAffinity> topAffinities(long studentId, int n);

	/**
	 * Abandons an opened career: 409 if no page was viewed (or already reflected / abandoned for good), 404 if unknown
	 * or inactive. Applies the abandon penalty to each tag and clears the in-progress card, in one transaction.
	 */
	void abandon(long studentId, long careerId);

	/** Abandon totals per student, in one query; students who never abandoned are absent from the map. */
	java.util.Map<Long, AbandonActivity> abandonActivityOf(java.util.Collection<Long> studentIds);

	/** The student's abandoned careers (for counselor views), most recent first. */
	List<AbandonedCareer> abandonedCareers(long studentId);

}
