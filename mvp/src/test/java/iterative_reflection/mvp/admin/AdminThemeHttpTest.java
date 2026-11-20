package iterative_reflection.mvp.admin;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class AdminThemeHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	private static final String URI = "/api/admin/settings/theme";

	@AfterEach
	void clearSetting() {
		jdbc.update("delete from app_settings");
	}

	private Session as(String role) {
		return signIn(createUser(uniqueEmail(), PASSWORD, role, false), PASSWORD);
	}

	private ResponseSpec put(Session s, Object body) {
		return http.put().uri(URI).headers(s::apply).contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	@Test
	void getHasNullsBeforeAnySave() {
		http.get().uri(URI).headers(as("ADMIN")::apply).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.presetId").isEqualTo("grove").jsonPath("$.updatedAt").isEmpty()
				.jsonPath("$.updatedByName").isEmpty();
	}

	@Test
	void putSavesAndBothGetsReflectIt() {
		Session admin = as("ADMIN");
		put(admin, Map.of("presetId", "harbor")).expectStatus().isOk().expectBody().jsonPath("$.presetId")
				.isEqualTo("harbor").jsonPath("$.updatedAt").isNotEmpty().jsonPath("$.updatedByName")
				.isEqualTo("Test User");
		http.get().uri(URI).headers(admin::apply).exchange().expectBody().jsonPath("$.presetId").isEqualTo("harbor")
				.jsonPath("$.updatedAt").isNotEmpty().jsonPath("$.updatedByName").isEqualTo("Test User");
		http.get().uri("/api/settings/theme").exchange().expectBody().jsonPath("$.presetId").isEqualTo("harbor");
	}

	@Test
	void anUnknownIdIs400WithAFieldError() {
		Session admin = as("ADMIN");
		put(admin, Map.of("presetId", "neon")).expectStatus().isBadRequest().expectBody()
				.jsonPath("$.errors[0].field").isEqualTo("presetId");
		put(admin, Map.of()).expectStatus().isBadRequest().expectBody().jsonPath("$.errors[0].field")
				.isEqualTo("presetId");
		http.get().uri("/api/settings/theme").exchange().expectBody().jsonPath("$.presetId").isEqualTo("grove");
	}

	@Test
	void nonAdminsGet403() {
		for (String role : new String[] { "STUDENT", "COUNSELOR" }) {
			Session s = as(role);
			put(s, Map.of("presetId", "plum")).expectStatus().isForbidden();
			http.get().uri(URI).headers(s::apply).exchange().expectStatus().isForbidden();
		}
	}

	@Test
	void putWithoutCsrfIs403() {
		Session admin = as("ADMIN");
		http.put().uri(URI).header("Cookie", "SESSION=" + admin.sessionId()).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("presetId", "plum")).exchange().expectStatus().isForbidden();
	}

	@Test
	void savingTheCurrentValueKeepsUpdatedAt() {
		Session admin = as("ADMIN");
		Object first = put(admin, Map.of("presetId", "slate")).expectStatus().isOk().returnResult(Map.class)
				.getResponseBody().get("updatedAt");
		Object second = put(admin, Map.of("presetId", "slate")).expectStatus().isOk().returnResult(Map.class)
				.getResponseBody().get("updatedAt");
		org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
	}

}
