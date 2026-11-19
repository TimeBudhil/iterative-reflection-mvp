/**
 * Feed: the stored in-progress card per student, candidate pool, card picking, the exhausted state, and the 409 guard
 * that blocks opening a career other than the in-progress one. Public API: {@link iterative_reflection.mvp.feed.FeedService}
 * and {@link iterative_reflection.mvp.feed.FeedCard}. Depends on career and reflection (never the reverse).
 */
package iterative_reflection.mvp.feed;
