package iterative_reflection.mvp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class AdminCounselorHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	private static final Pattern JOIN_CODE = Pattern.compile("[A-HJ-NP-Z2-9]{6,8}");

	private ResponseSpec create(Session s, String email, String name, String tempPassword) {
		return http.post().uri("/api/admin/counselors").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", email, "displayName", name, "temporaryPassword", tempPassword)).exchange();
	}

	private Session admin() {
		return signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
	}

	@Test
	void adminCreatesCounselorWithJoinCodeAndMustChangePassword() {
		String email = uniqueEmail();
		create(admin(), email, "Ms Counselor", "temporary-password-1").expectStatus().isCreated()
				.expectBody()
				.jsonPath("$.email").isEqualTo(email)
				.jsonPath("$.displayName").isEqualTo("Ms Counselor")
				.jsonPath("$.mustChangePassword").isEqualTo(true)
				.jsonPath("$.active").isEqualTo(true)
				.jsonPath("$.joinCode").value(code -> assertThat((String) code).matches(JOIN_CODE));
		signIn(email, "temporary-password-1");
	}

	@Test
	void nonAdminGets403() {
		create(signedInStudent(), uniqueEmail(), "X", "temporary-password-1").expectStatus().isForbidden();
		Session counselor = signIn(createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false), PASSWORD);
		create(counselor, uniqueEmail(), "X", "temporary-password-1").expectStatus().isForbidden();
	}

	@Test
	void duplicateEmailIs409AndWeakTemporaryPasswordIs400() {
		Session admin = admin();
		String email = uniqueEmail();
		create(admin, email, "A", "temporary-password-1").expectStatus().isCreated();
		create(admin, email.toUpperCase(), "B", "temporary-password-1").expectStatus().isEqualTo(409);
		create(admin, uniqueEmail(), "C", "short").expectStatus().isBadRequest();
	}

	@Test
	void joinCodesAreUniqueAndUnambiguous() {
		Session admin = admin();
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 25; i++) {
			create(admin, uniqueEmail(), "C" + i, "temporary-password-1").expectBody()
					.jsonPath("$.joinCode").value(c -> {
						String code = (String) c;
						assertThat(code).matches(JOIN_CODE).doesNotContainPattern("[01OI]");
						codes.add(code);
					});
		}
		assertThat(codes).hasSize(25);
	}

}
