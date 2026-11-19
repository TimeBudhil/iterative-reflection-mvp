/**
 * Intake: DB-defined intake questions, one editable response per student, dismissal and the "intake prompt due"
 * rule. Public API: {@link iterative_reflection.mvp.intake.IntakeSaved} (published synchronously inside the saving
 * transaction) and {@link iterative_reflection.mvp.intake.IntakeReader} (read-only, for counselor and admin modules).
 * Implements identity's {@code IntakePromptProvider} for {@code GET /api/me}.
 */
package iterative_reflection.mvp.intake;
