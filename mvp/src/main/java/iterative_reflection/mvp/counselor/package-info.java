/**
 * Counselor: profiles and join codes (later: links, dashboard, audit). Public API:
 * {@link iterative_reflection.mvp.counselor.CounselorService} and
 * {@link iterative_reflection.mvp.counselor.CounselorProfile}. Depends on identity (the signed-in principal);
 * identity must never call counselor directly (use an event) or the modules form a cycle.
 */
package iterative_reflection.mvp.counselor;
