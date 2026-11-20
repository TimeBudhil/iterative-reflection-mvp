package iterative_reflection.mvp.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.test.context.ActiveProfiles;

/** Boots only the settings module (it depends on no other feature module). */
@ApplicationModuleTest
@ActiveProfiles("dev")
class SettingsModuleTest {

	@Autowired
	ThemeSettings theme;

	@Autowired
	JdbcTemplate jdbc;

	@AfterEach
	void clear() {
		jdbc.update("delete from app_settings");
	}

	@Test
	void bootsAloneAndDefaultsToGrove() {
		assertThat(theme.currentPresetId()).isEqualTo("grove");
	}

	@Test
	void aChangingSavePublishesSettingChanged(PublishedEvents events) {
		SavedTheme saved = theme.save("harbor", 42L);

		assertThat(saved.presetId()).isEqualTo("harbor");
		assertThat(theme.currentPresetId()).isEqualTo("harbor");
		var published = java.util.stream.StreamSupport.stream(events.ofType(SettingChanged.class).spliterator(), false)
				.toList();
		assertThat(published).hasSize(1);
		SettingChanged event = published.get(0);
		assertThat(event.key()).isEqualTo("app.theme");
		assertThat(event.oldValue()).isEqualTo("grove");
		assertThat(event.newValue()).isEqualTo("harbor");
		assertThat(event.adminId()).isEqualTo(42L);
		assertThat(event.changedAt()).isEqualTo(saved.updatedAt());
	}

	@Test
	void aSameValueSavePublishesNothingAndKeepsUpdatedAt(PublishedEvents events) {
		SavedTheme first = theme.save("plum", 1L);
		SavedTheme again = theme.save("plum", 2L);

		assertThat(again).isEqualTo(first);
		assertThat(events.ofType(SettingChanged.class)).hasSize(1);
	}

	@Test
	void anUnknownPresetIsRejectedAndPublishesNothing(PublishedEvents events) {
		assertThatThrownBy(() -> theme.save("neon", 1L)).isInstanceOf(IllegalArgumentException.class);
		assertThat(events.ofType(SettingChanged.class)).isEmpty();
	}

}
