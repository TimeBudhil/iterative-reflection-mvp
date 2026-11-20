package iterative_reflection.mvp.admin.internal.dto;

import java.time.Instant;
import java.util.Map;

/** One admin activity row as the API returns it; {@code id} is unique (a stable list key); {@code adminName} is null if the admin no longer exists. */
public record ActivityEntry(long id, Instant createdAt, String adminName, String action, Map<String, String> detail) {
}
