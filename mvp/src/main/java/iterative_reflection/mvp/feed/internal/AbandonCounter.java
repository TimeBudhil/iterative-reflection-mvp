package iterative_reflection.mvp.feed.internal;

/**
 * Number of abandons that count toward the exploration slot: slot = (reflections + abandons) mod exploration-every.
 * Counts every abandon event (a resurfaced career abandoned again counts twice).
 */
interface AbandonCounter {

	long abandons(long studentId);

}
