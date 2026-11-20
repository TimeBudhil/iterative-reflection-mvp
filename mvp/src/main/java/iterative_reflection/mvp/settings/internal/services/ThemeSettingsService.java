package iterative_reflection.mvp.settings.internal.services;

import iterative_reflection.mvp.settings.SavedTheme;
import iterative_reflection.mvp.settings.SettingChanged;
import iterative_reflection.mvp.settings.ThemeSettings;
import iterative_reflection.mvp.settings.internal.repository.AppSettingsRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
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

	private final ApplicationEventPublisher events;

	private final Clock clock;

	ThemeSettingsService(AppSettingsRepository repository, JsonMapper json, ApplicationEventPublisher events,
			ObjectProvider<Clock> clock) {
		this.repository = repository;
		this.json = json;
		this.events = events;
		// the platform Clock bean is absent when this module is booted alone
		this.clock = clock.getIfAvailable(Clock::systemUTC);
	}

	@Override
	@Transactional(readOnly = true)
	public String currentPresetId() {
		return repository.findValueJson(KEY).map(this::parsePresetId).filter(ThemeSettings::isKnownPreset)
				.orElse(DEFAULT_PRESET_ID);
	}

	@Override
	@Transactional(readOnly = true)
	public SavedTheme saved() {
		SavedTheme none = new SavedTheme(DEFAULT_PRESET_ID, null, null);
		return repository.find(KEY).map(row -> {
			String id = parsePresetId(row.valueJson());
			return ThemeSettings.isKnownPreset(id) ? new SavedTheme(id, row.updatedAt(), row.updatedBy()) : none;
		}).orElse(none);
	}

	@Override
	@Transactional
	public SavedTheme save(String presetId, long adminId) {
		if (!ThemeSettings.isKnownPreset(presetId)) {
			throw new IllegalArgumentException("Unknown theme preset: " + presetId);
		}
		SavedTheme before = saved();
		if (before.presetId().equals(presetId)) {
			return before;
		}
		Instant now = clock.instant();
		repository.upsert(KEY, json.writeValueAsString(Map.of("presetId", presetId)), adminId, now);
		events.publishEvent(new SettingChanged(KEY, before.presetId(), presetId, adminId, now));
		return new SavedTheme(presetId, now, adminId);
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
