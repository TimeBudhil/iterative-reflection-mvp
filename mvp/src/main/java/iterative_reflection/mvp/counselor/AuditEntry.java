package iterative_reflection.mvp.counselor;

import java.time.Instant;

/** One counselor detail read. {@code outcome} is ALLOWED or DENIED; {@code at} is a UTC instant. */
public record AuditEntry(long id, long counselorId, long studentId, String outcome, Instant at) {
}
