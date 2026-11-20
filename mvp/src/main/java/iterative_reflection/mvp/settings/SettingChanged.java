package iterative_reflection.mvp.settings;

import java.time.Instant;

/**
 * Published (via {@code ApplicationEventPublisher}) inside the saving transaction whenever a setting's value
 * actually changes; a same-value save publishes nothing. {@code oldValue} and {@code newValue} are the typed
 * setting's plain value (for {@code app.theme}, the preset id). Listeners that must not affect the save (such as the
 * admin activity log) should use {@code @TransactionalEventListener(phase = AFTER_COMMIT)}.
 */
public record SettingChanged(String key, String oldValue, String newValue, long adminId, Instant changedAt) {
}
