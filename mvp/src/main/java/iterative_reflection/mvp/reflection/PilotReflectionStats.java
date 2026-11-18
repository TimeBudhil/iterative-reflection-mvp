package iterative_reflection.mvp.reflection;

/** Pilot-cohort reflection aggregates: students with enough reflections and current-version verdict counts. */
public record PilotReflectionStats(long studentsWithMinReflections, long yes, long maybe, long no) {
}
