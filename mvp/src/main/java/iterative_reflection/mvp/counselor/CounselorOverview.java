package iterative_reflection.mvp.counselor;

/** Admin-facing counselor facts: join code and number of students with an active link. */
public record CounselorOverview(String joinCode, long linkedStudentCount) {
}
