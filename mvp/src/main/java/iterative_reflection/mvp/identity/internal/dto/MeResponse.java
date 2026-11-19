package iterative_reflection.mvp.identity.internal.dto;

import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.identity.UserSummary;

public record MeResponse(long id, String email, String displayName, Role role, boolean mustChangePassword,
		boolean intakePromptDue) {

	public static MeResponse of(UserSummary u, boolean intakePromptDue) {
		return new MeResponse(u.id(), u.email(), u.displayName(), u.role(), u.mustChangePassword(),
				intakePromptDue);
	}

}
