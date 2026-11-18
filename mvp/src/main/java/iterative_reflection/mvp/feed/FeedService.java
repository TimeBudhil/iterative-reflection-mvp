package iterative_reflection.mvp.feed;

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

}
