package iterative_reflection.mvp.counselor;

/** A counselor's profile row: the identity user id and the current join code. */
public record CounselorProfile(long userId, String joinCode) {
}
