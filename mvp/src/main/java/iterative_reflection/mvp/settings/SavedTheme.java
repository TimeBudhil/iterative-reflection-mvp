package iterative_reflection.mvp.settings;

import java.time.Instant;

/** The saved theme with who and when; {@code updatedAt} and {@code updatedBy} are null before any save. */
public record SavedTheme(String presetId, Instant updatedAt, Long updatedBy) {
}
