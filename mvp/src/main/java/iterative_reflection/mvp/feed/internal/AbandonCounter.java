package iterative_reflection.mvp.feed.internal;

/**
 * Number of abandons that count toward the exploration slot: slot = (reflections + abandons) mod exploration-every.
 * Ticket 12 deletes {@link NoAbandons} and supplies the real implementation (count of the student's abandons rows).
 */
interface AbandonCounter {

	long abandons(long studentId);

}
