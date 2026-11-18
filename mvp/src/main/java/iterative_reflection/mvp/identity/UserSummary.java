package iterative_reflection.mvp.identity;

/** Read-only view of a user, safe to hand to other modules (no credentials). */
public record UserSummary(long id, String email, String displayName, Role role, boolean active,
		boolean mustChangePassword) {
}
