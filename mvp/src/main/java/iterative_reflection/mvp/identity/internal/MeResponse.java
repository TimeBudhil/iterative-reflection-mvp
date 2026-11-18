package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.identity.UserSummary;

record MeResponse(long id, String email, String displayName, Role role, boolean mustChangePassword,
		boolean intakePromptDue) {

	static MeResponse of(UserSummary u, boolean intakePromptDue) {
		return new MeResponse(u.id(), u.email(), u.displayName(), u.role(), u.mustChangePassword(),
				intakePromptDue);
	}

}
