package iterative_reflection.mvp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.counselor.CounselorService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class AdminCounselorManagementHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@Autowired
	private CounselorService counselors;

	private record Counselor(long id, String code, String email) {
	}

	private Counselor newCounselor() {
		String email = createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);
		long id = idOf(email);
		return new Counselor(id, counselors.createProfile(id).joinCode(), email);
	}

	private long idOf(String email) {
		return jdbc.queryForObject("select id from users where email = ?", Long.class, email);
	}

	private Session admin() {
		return signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
	}

	private ResponseSpec setActive(Session s, long id, boolean active) {
		return http.patch().uri("/api/admin/counselors/" + id).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("active", active)).exchange();
	}

	private ResponseSpec link(Session s, String code) {
		return http.post().uri("/api/students/me/counselors").headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("code", code)).exchange();
	}

	@Test
	void listShowsCounselorFieldsAndLinkedStudentCount() {
		Counselor c = newCounselor();
		link(signedInStudent(), c.code()).expectStatus().isCreated();
		link(signedInStudent(), c.code()).expectStatus().isCreated();
		String row = "$.content[?(@.id==" + c.id() + ")]";
		http.get().uri("/api/admin/counselors?size=100").headers(admin()::apply).exchange().expectStatus().isOk()
				.expectBody()
				.jsonPath("$.page.size").isEqualTo(100)
				.jsonPath(row + ".email").isEqualTo(c.email())
				.jsonPath(row + ".joinCode").isEqualTo(c.code())
				.jsonPath(row + ".active").isEqualTo(true)
				.jsonPath(row + ".linkedStudentCount").isEqualTo(2)
				.jsonPath(row + ".displayName").exists();
	}

	@Test
	void listIsPaginated() {
		newCounselor();
		newCounselor();
		http.get().uri("/api/admin/counselors?size=1").headers(admin()::apply).exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.content.length()").isEqualTo(1).jsonPath("$.page.totalPages")
				.value(n -> assertThat(((Number) n).intValue()).isGreaterThanOrEqualTo(2));
	}

	@Test
	void deactivationKillsSessionsBlocksSignInAndKeepsLinks() {
		Counselor c = newCounselor();
		String studentEmail = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		Session student = signIn(studentEmail, PASSWORD);
		link(student, c.code()).expectStatus().isCreated();
		Session counselorSession = signIn(c.email(), PASSWORD);
		Session admin = admin();

		setActive(admin, c.id(), false).expectStatus().isOk().expectBody().jsonPath("$.active").isEqualTo(false);

		http.get().uri("/api/counselor/me").headers(counselorSession::apply).exchange().expectStatus()
				.isUnauthorized();
		signin(c.email(), PASSWORD, null).expectStatus().isUnauthorized();
		assertThat(counselors.canSee(c.id(), idOf(studentEmail))).isFalse();
		http.get().uri("/api/students/me/counselors").headers(student::apply).exchange().expectBody()
				.jsonPath("$.content[0].counselorActive").isEqualTo(false);
		link(signedInStudent(), c.code()).expectStatus().isNotFound();

		setActive(admin, c.id(), true).expectStatus().isOk().expectBody().jsonPath("$.active").isEqualTo(true);
		signIn(c.email(), PASSWORD);
		assertThat(counselors.canSee(c.id(), idOf(studentEmail))).isTrue();
		http.get().uri("/api/students/me/counselors").headers(student::apply).exchange().expectBody()
				.jsonPath("$.content[0].counselorActive").isEqualTo(true);
		link(signedInStudent(), c.code()).expectStatus().isCreated();
	}

	@Test
	void nonCounselorTargetIs404AndMissingFieldIs400() {
		Session admin = admin();
		long studentId = idOf(createUser(uniqueEmail(), PASSWORD, "STUDENT", false));
		setActive(admin, studentId, false).expectStatus().isNotFound();
		setActive(admin, 999999999L, false).expectStatus().isNotFound();
		Counselor c = newCounselor();
		http.patch().uri("/api/admin/counselors/" + c.id()).headers(admin::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of()).exchange().expectStatus().isBadRequest();
	}

	@Test
	void nonAdminsGet403() {
		Counselor c = newCounselor();
		Session student = signedInStudent();
		Session counselor = signIn(c.email(), PASSWORD);
		for (Session s : new Session[] { student, counselor }) {
			http.get().uri("/api/admin/counselors").headers(s::apply).exchange().expectStatus().isForbidden();
			setActive(s, c.id(), false).expectStatus().isForbidden();
		}
	}

}
