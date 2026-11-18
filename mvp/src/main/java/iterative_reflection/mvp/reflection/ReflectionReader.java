package iterative_reflection.mvp.reflection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only public API of the reflection module (consumed by feed, intake and counselor; admin may extend it). */
public interface ReflectionReader {

	/** Ids of every career the student has reflected on (including retired careers). */
	Set<Long> reflectedCareerIds(long studentId);

	/** Number of careers the student has reflected on (abandons never count). */
	int reflectionCount(long studentId);

	/** Every reflection of the student at its current version, most recently updated first. */
	List<ReflectionRecord> reflectionsOf(long studentId);

	/** Totals per student, in one query; students with no reflection are absent from the map. */
	Map<Long, ReflectionActivity> activityOf(Collection<Long> studentIds);

}
