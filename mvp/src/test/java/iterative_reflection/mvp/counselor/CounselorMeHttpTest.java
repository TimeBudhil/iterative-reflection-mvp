package iterative_reflection.mvp.counselor;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CounselorMeHttpTest extends HttpIntegrationTest {

	private static final String TEMP = "temporary-password-1";

	private static final String NEW = "another long passphrase";

	@Test
	void counselorSeesProfileAndJoinCodeOnlyAfterChangingTemporaryPassword() {
		Session admin = signIn(createUser(uniqueEmail(), "correct horse battery", "ADMIN", false),
				"correct horse battery");
		String email = uniqueEmail();
		String code = (String) http.post().uri("/api/admin/counselors").headers(admin::apply)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", email, "displayName", "Ms C", "temporaryPassword", TEMP)).exchange()
				.expectStatus().isCreated().returnResult(Map.class).getResponseBody().get("joinCode");

		Session first = signIn(email, TEMP);
		// R1.10 gate: everything but me / password change / signout is 403
		http.get().uri("/api/counselor/me").headers(first::apply).exchange().expectStatus().isForbidden();
		http.get().uri("/api/me").headers(first::apply).exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.mustChangePassword").isEqualTo(true);

		http.put().uri("/api/me/password").headers(first::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("currentPassword", TEMP, "newPassword", NEW)).exchange().expectStatus().isNoContent();

		Session second = signIn(email, NEW);
		http.get().uri("/api/counselor/me").headers(second::apply).exchange().expectStatus().isOk()
				.expectBody()
				.jsonPath("$.email").isEqualTo(email)
				.jsonPath("$.displayName").isEqualTo("Ms C")
				.jsonPath("$.joinCode").isEqualTo(code);
		http.get().uri("/api/me").headers(second::apply).exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.mustChangePassword").isEqualTo(false);
	}

	@Test
	void signOutWorksWhileMustChangePassword() {
		String email = createUser(uniqueEmail(), TEMP, "COUNSELOR", true);
		Session s = signIn(email, TEMP);
		http.post().uri("/api/auth/signout").headers(s::apply).exchange().expectStatus().isNoContent();
		http.get().uri("/api/me").headers(s::apply).exchange().expectStatus().isUnauthorized();
	}

	@Test
	void studentAndAnonymousCannotReadCounselorMe() {
		http.get().uri("/api/counselor/me").headers(signedInStudent()::apply).exchange().expectStatus().isForbidden();
		http.get().uri("/api/counselor/me").exchange().expectStatus().isUnauthorized();
	}

}
