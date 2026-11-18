package iterative_reflection.mvp.identity;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class AccountSettingsHttpTest extends HttpIntegrationTest {

	private static final String OLD = "correct horse battery";

	private static final String NEW = "another long passphrase";

	private ResponseSpec changePassword(Session s, String current, String next) {
		return http.put().uri("/api/me/password").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("currentPassword", current, "newPassword", next)).exchange();
	}

	@Test
	void changePasswordReturns204AndNewPasswordWorks() {
		String email = createUser(uniqueEmail(), OLD, "STUDENT", false);
		Session s = signIn(email, OLD);
		changePassword(s, OLD, NEW).expectStatus().isNoContent();
		signin(email, OLD, null).expectStatus().isUnauthorized();
		signin(email, NEW, null).expectStatus().isOk();
	}

	@Test
	void wrongCurrentPasswordIs400AndPasswordUnchanged() {
		String email = createUser(uniqueEmail(), OLD, "STUDENT", false);
		Session s = signIn(email, OLD);
		changePassword(s, "not the password", NEW).expectStatus().isBadRequest()
				.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
		signin(email, OLD, null).expectStatus().isOk();
	}

	@Test
	void newPasswordMustFollowLengthRules() {
		Session s = signedInStudent();
		changePassword(s, OLD, "12345678901").expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors[?(@.field == 'newPassword')]").exists();
		changePassword(s, OLD, "x".repeat(73)).expectStatus().isBadRequest();
	}

	@Test
	void sessionIdRotatesAndOldSessionIsInvalid() {
		String email = createUser(uniqueEmail(), OLD, "STUDENT", false);
		Session s = signIn(email, OLD);
		ResponseSpec r = changePassword(s, OLD, NEW);
		r.expectStatus().isNoContent();
		String newId = r.returnResult().getResponseHeaders().get("Set-Cookie").stream()
				.filter(c -> c.startsWith("SESSION=")).map(c -> c.substring(8).split(";", 2)[0]).findFirst()
				.orElseThrow();
		assertThat(newId).isNotEqualTo(s.sessionId());
		http.get().uri("/api/me").headers(s::apply).exchange().expectStatus().isUnauthorized();
		http.get().uri("/api/me").headers(h -> h.set("Cookie", "SESSION=" + newId)).exchange().expectStatus().isOk();
	}

	@Test
	void changingPasswordEndsEveryOtherSessionOfTheUser() {
		String email = createUser(uniqueEmail(), OLD, "STUDENT", false);
		Session laptop = signIn(email, OLD);
		Session phone = signIn(email, OLD);
		changePassword(laptop, OLD, NEW).expectStatus().isNoContent();
		http.get().uri("/api/me").headers(phone::apply).exchange().expectStatus().isUnauthorized();
		Session fresh = signIn(email, NEW);
		http.get().uri("/api/me").headers(fresh::apply).exchange().expectStatus().isOk();
	}

	@Test
	void changePasswordClearsMustChangePassword() {
		String email = createUser(uniqueEmail(), OLD, "COUNSELOR", true);
		Session s = signIn(email, OLD);
		changePassword(s, OLD, NEW).expectStatus().isNoContent();
		Session fresh = signIn(email, NEW);
		http.get().uri("/api/me").headers(fresh::apply).exchange().expectBody().jsonPath("$.mustChangePassword")
				.isEqualTo(false);
	}

	@Test
	void changePasswordRequiresSignIn() {
		http.put().uri("/api/me/password").headers(csrf()::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("currentPassword", OLD, "newPassword", NEW)).exchange().expectStatus().isUnauthorized();
	}

	@Test
	void patchMeUpdatesDisplayNameAndIgnoresEmailAndRole() {
		String email = createUser(uniqueEmail(), OLD, "STUDENT", false);
		Session s = signIn(email, OLD);
		http.patch().uri("/api/me").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("displayName", "  New Name ", "email", "evil@test.example", "role", "ADMIN"))
				.exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.displayName").isEqualTo("New Name")
				.jsonPath("$.email").isEqualTo(email).jsonPath("$.role").isEqualTo("STUDENT");
		http.get().uri("/api/me").headers(s::apply).exchange().expectBody()
				.jsonPath("$.displayName").isEqualTo("New Name").jsonPath("$.role").isEqualTo("STUDENT");
	}

	@Test
	void blankDisplayNameIs400() {
		Session s = signedInStudent();
		http.patch().uri("/api/me").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("displayName", "   ")).exchange().expectStatus().isBadRequest()
				.expectBody().jsonPath("$.errors[?(@.field == 'displayName')]").exists();
	}

}
