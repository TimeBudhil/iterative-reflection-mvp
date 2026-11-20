package iterative_reflection.mvp.admin.internal.integration;

import iterative_reflection.mvp.admin.internal.services.AdminActivity;
import iterative_reflection.mvp.settings.SettingChanged;
import iterative_reflection.mvp.settings.ThemeSettings;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Logs {@code THEME_CHANGED} after the settings save commits (best-effort, B-R11.6). */
@Component
class ThemeActivityListener {

	private final AdminActivity activity;

	ThemeActivityListener(AdminActivity activity) {
		this.activity = activity;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void on(SettingChanged event) {
		if (ThemeSettings.SETTING_KEY.equals(event.key())) {
			activity.record(event.adminId(), AdminActivity.Action.THEME_CHANGED,
					Map.of("from", event.oldValue(), "to", event.newValue()));
		}
	}

}
