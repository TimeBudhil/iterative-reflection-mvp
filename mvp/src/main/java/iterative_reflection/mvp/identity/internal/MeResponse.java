package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.identity.UserSummary;

record MeResponse(long id, String email, String displayName, Role role, boolean intakePromptDue) {

	/** {@code intakePromptDue} is always false until the intake module exists (ticket 13). */
	static MeResponse of(UserSummary u) {
		return new MeResponse(u.id(), u.email(), u.displayName(), u.role(), false);
	}

}
