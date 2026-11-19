package iterative_reflection.mvp.admin.internal.dto;

/** Pilot success metrics [R9.5]. Percentages are 0..100 rounded to two decimals; 0 when there are no students. */
public record PilotMetrics(long totalStudents, long studentsWithThreeReflections, double percentWithThreeReflections,
		long studentsWithIntake, double percentIntakeCompleted, Verdicts verdicts) {

	public record Verdicts(long YES, long MAYBE, long NO) {
	}

}
