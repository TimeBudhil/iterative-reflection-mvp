package iterative_reflection.mvp.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots only the settings module (it depends on no other feature module). Ticket 03 extends this with the
 * {@code SettingChanged} publish / no-publish assertions.
 */
@ApplicationModuleTest
@ActiveProfiles("dev")
class SettingsModuleTest {

	@Autowired
	ThemeSettings theme;

	@Test
	void bootsAloneAndDefaultsToGrove() {
		assertThat(theme.currentPresetId()).isEqualTo("grove");
	}

}
