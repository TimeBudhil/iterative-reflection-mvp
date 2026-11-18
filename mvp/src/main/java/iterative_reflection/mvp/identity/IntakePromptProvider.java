package iterative_reflection.mvp.identity;

/**
 * Implemented by the intake module so that {@code GET /api/me} can report {@code intakePromptDue} without identity
 * depending on intake (intake depends on identity, never the reverse).
 */
public interface IntakePromptProvider {

	/** True when the student has ≥3 reflections and neither submitted nor dismissed the intake [R7.1, R7.3]. */
	boolean intakePromptDue(long studentId);

}
