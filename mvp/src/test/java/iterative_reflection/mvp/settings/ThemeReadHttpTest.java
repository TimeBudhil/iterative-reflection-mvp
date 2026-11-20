package iterative_reflection.mvp.settings;

import iterative_reflection.mvp.HttpIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;

class ThemeReadHttpTest extends HttpIntegrationTest {

	@AfterEach
	void clearSetting() {
		jdbc.update("delete from app_settings");
	}

	@Test
	void defaultsToGroveWhenNothingIsSaved() {
		http.get().uri("/api/settings/theme").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.presetId").isEqualTo("grove");
	}

	@Test
	void worksSignedOutAndRevealsNothingButThePresetId() {
		http.get().uri("/api/settings/theme").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.length()").isEqualTo(1).jsonPath("$.updatedAt").doesNotExist()
				.jsonPath("$.updatedByName").doesNotExist();
	}

	@Test
	void sendsNoCache() {
		http.get().uri("/api/settings/theme").exchange().expectHeader().cacheControl(CacheControl.noCache());
	}

	@Test
	void returnsTheSavedPreset() {
		save("{\"presetId\":\"harbor\"}");
		http.get().uri("/api/settings/theme").exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.presetId").isEqualTo("harbor");
	}

	@Test
	void anUnknownOrMalformedStoredValueFallsBackToGrove() {
		save("{\"presetId\":\"neon\"}");
		http.get().uri("/api/settings/theme").exchange().expectBody().jsonPath("$.presetId").isEqualTo("grove");
		jdbc.update("delete from app_settings");
		save("not json");
		http.get().uri("/api/settings/theme").exchange().expectBody().jsonPath("$.presetId").isEqualTo("grove");
	}

	@Test
	void onlyGetIsPublic() {
		http.put().uri("/api/settings/theme").exchange().expectStatus().isForbidden();
		http.post().uri("/api/settings/theme").exchange().expectStatus().isForbidden();
	}

	private void save(String valueJson) {
		jdbc.update("insert into app_settings (\"key\", value_json, updated_by, updated_at) values "
				+ "('app.theme', ?, null, current_timestamp)", valueJson);
	}

}
