package iterative_reflection.mvp.counselor.internal.services;

/** The counselor may not see this student (not linked, revoked, or no such student: deliberately the same answer). */
public class StudentAccessDeniedException extends RuntimeException {

	public StudentAccessDeniedException() {
		super("Access is denied.");
	}

}
