package iterative_reflection.mvp.feed.internal.services;

/**
 * Number of abandons that count toward the exploration slot: slot = (reflections + abandons) mod exploration-every.
 * Counts every abandon event (a resurfaced career abandoned again counts twice).
 */
public interface AbandonCounter {

	long abandons(long studentId);

}
