package iterative_reflection.mvp.settings.internal.services;

import iterative_reflection.mvp.settings.ThemeSettings;
import iterative_reflection.mvp.settings.internal.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Typed access to the {@code app.theme} setting, stored as {@code {"presetId":"..."}}. */
@Service
class ThemeSettingsService implements ThemeSettings {

	static final String KEY = "app.theme";

	private final AppSettingsRepository repository;

	private final JsonMapper json;

	ThemeSettingsService(AppSettingsRepository repository, JsonMapper json) {
		this.repository = repository;
		this.json = json;
	}

	@Override
	@Transactional(readOnly = true)
	public String currentPresetId() {
		return repository.findValueJson(KEY).map(this::parsePresetId).filter(ThemeSettings::isKnownPreset)
				.orElse(DEFAULT_PRESET_ID);
	}

	private String parsePresetId(String valueJson) {
		try {
			JsonNode node = json.readTree(valueJson).path("presetId");
			return node.isString() ? node.stringValue() : null;
		}
		catch (RuntimeException e) {
			return null;
		}
	}

}
