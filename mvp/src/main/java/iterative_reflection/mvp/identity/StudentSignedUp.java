package iterative_reflection.mvp.identity;

/**
 * Published inside the signup transaction after the student row exists. A listener that throws (for example an
 * unknown {@code counselorCode}) rolls the whole signup back. {@code counselorCode} is null when none was given.
 */
public record StudentSignedUp(long userId, String counselorCode) {
}
