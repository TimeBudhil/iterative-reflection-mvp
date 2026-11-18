package iterative_reflection.mvp.reflection;

import java.util.Set;

/** Read-only public API of the reflection module (consumed by feed and intake; counselor/admin may extend it). */
public interface ReflectionReader {

	/** Ids of every career the student has reflected on (including retired careers). */
	Set<Long> reflectedCareerIds(long studentId);

	/** Number of careers the student has reflected on (abandons never count). */
	int reflectionCount(long studentId);

}
