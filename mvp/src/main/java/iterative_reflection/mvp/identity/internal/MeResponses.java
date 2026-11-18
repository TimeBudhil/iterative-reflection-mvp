package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.IntakePromptProvider;
import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.identity.UserSummary;
import org.springframework.stereotype.Component;

/** Builds {@link MeResponse}s, asking the intake module (through {@link IntakePromptProvider}) for the prompt flag. */
@Component
class MeResponses {

	private final IntakePromptProvider intakePrompt;

	MeResponses(IntakePromptProvider intakePrompt) {
		this.intakePrompt = intakePrompt;
	}

	MeResponse of(UserSummary u) {
		return MeResponse.of(u, u.role() == Role.STUDENT && intakePrompt.intakePromptDue(u.id()));
	}

}
