package iterative_reflection.mvp.settings;

import java.util.List;

/**
 * Public API of the settings module for the app theme. Only a preset id is stored (never colors); the colors live in
 * the frontend, next to their contrast tests.
 */
public interface ThemeSettings {

	/** The {@link SettingChanged#key()} this setting publishes under. */
	String SETTING_KEY = "app.theme";

	String DEFAULT_PRESET_ID = "grove";

	/** The presets the app offers, default first. */
	List<String> PRESET_IDS = List.of(DEFAULT_PRESET_ID, "harbor", "plum", "slate");

	/** The saved preset id; {@link #DEFAULT_PRESET_ID} when nothing is saved or the stored id is not a known preset. */
	String currentPresetId();

	/** The effective preset plus who last saved it and when (nulls when nothing was ever saved). */
	SavedTheme saved();

	/**
	 * Saves the preset as {@code adminId}. A change publishes {@link SettingChanged}; saving the current value
	 * changes nothing and publishes nothing. Throws {@link IllegalArgumentException} for an unknown id.
	 */
	SavedTheme save(String presetId, long adminId);

	static boolean isKnownPreset(String presetId) {
		return presetId != null && PRESET_IDS.contains(presetId);
	}

}
