package iterative_reflection.mvp.counselor;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class StudentCounselorLinkHttpTest extends HttpIntegrationTest {

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

	private ResponseSpec link(Session s, String code) {
		return http.post().uri("/api/students/me/counselors").headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("code", code)).exchange();
	}

	private ResponseSpec signup(String email, String counselorCode) {
		Map<String, Object> body = new HashMap<>(Map.of("email", email, "password", PASSWORD, "displayName", "New",
				"birthdate", LocalDate.now(ZoneOffset.UTC).minusYears(20).toString(), "acceptedTermsVersion", "v1"));
		if (counselorCode != null) {
			body.put("counselorCode", counselorCode);
		}
		return http.post().uri("/api/auth/signup").headers(csrf()::apply).contentType(MediaType.APPLICATION_JSON)
				.body(body).exchange();
	}

	@Test
	void studentLinksByCodeAndSeesItListed() {
		Counselor c = newCounselor();
		Session s = signedInStudent();
		link(s, c.code()).expectStatus().isCreated().expectBody()
				.jsonPath("$.counselorId").isEqualTo((int) c.id())
				.jsonPath("$.counselorActive").isEqualTo(true);
		http.get().uri("/api/students/me/counselors").headers(s::apply).exchange().expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.content[0].counselorId").isEqualTo((int) c.id())
				.jsonPath("$.content[0].displayName").isEqualTo("Test User")
				.jsonPath("$.content[0].counselorActive").isEqualTo(true)
				.jsonPath("$.page.totalElements").isEqualTo(1);
	}

	@Test
	void codeIsCaseInsensitiveAndTrimmed() {
		Counselor c = newCounselor();
		link(signedInStudent(), " " + c.code().toLowerCase() + " ").expectStatus().isCreated();
	}

	@Test
	void unknownCodeIs404AndDuplicateIs409() {
		Counselor c = newCounselor();
		Session s = signedInStudent();
		link(s, "ZZZZZZZZ").expectStatus().isNotFound();
		link(s, c.code()).expectStatus().isCreated();
		link(s, c.code()).expectStatus().isEqualTo(409);
	}

	@Test
	void studentCanLinkToSeveralCounselors() {
		Session s = signedInStudent();
		link(s, newCounselor().code()).expectStatus().isCreated();
		link(s, newCounselor().code()).expectStatus().isCreated();
		http.get().uri("/api/students/me/counselors").headers(s::apply).exchange().expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(2);
	}

	@Test
	void listIsPaginated() {
		Session s = signedInStudent();
		for (int i = 0; i < 3; i++) {
			link(s, newCounselor().code()).expectStatus().isCreated();
		}
		http.get().uri("/api/students/me/counselors?size=2").headers(s::apply).exchange().expectBody()
				.jsonPath("$.content.length()").isEqualTo(2).jsonPath("$.page.totalPages").isEqualTo(2);
	}

	@Test
	void revokeRemovesAccessAndRelinkReactivatesTheSameLink() {
		Counselor c = newCounselor();
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		long studentId = idOf(email);
		Session s = signIn(email, PASSWORD);
		Number linkId = (Number) link(s, c.code()).expectStatus().isCreated().returnResult(Map.class)
				.getResponseBody().get("id");
		assertThat(counselors.canSee(c.id(), studentId)).isTrue();

		http.delete().uri("/api/students/me/counselors/" + linkId).headers(s::apply).exchange().expectStatus()
				.isNoContent();
		assertThat(counselors.canSee(c.id(), studentId)).isFalse();
		http.get().uri("/api/students/me/counselors").headers(s::apply).exchange().expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(0);
		http.delete().uri("/api/students/me/counselors/" + linkId).headers(s::apply).exchange().expectStatus()
				.isNotFound();

		link(s, c.code()).expectStatus().isCreated().expectBody().jsonPath("$.id").isEqualTo(linkId.intValue());
		assertThat(counselors.canSee(c.id(), studentId)).isTrue();
		assertThat(jdbc.queryForObject("select count(*) from counselor_links where student_id = ?", Long.class,
				studentId)).isEqualTo(1);
	}

	@Test
	void cannotRevokeAnotherStudentsLink() {
		Counselor c = newCounselor();
		Number linkId = (Number) link(signedInStudent(), c.code()).expectStatus().isCreated().returnResult(Map.class)
				.getResponseBody().get("id");
		http.delete().uri("/api/students/me/counselors/" + linkId).headers(signedInStudent()::apply).exchange()
				.expectStatus().isNotFound();
	}

	@Test
	void deactivatedCounselorGrantsNoAccessKeepsLinkAndRejectsCode() {
		Counselor c = newCounselor();
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		Session s = signIn(email, PASSWORD);
		link(s, c.code()).expectStatus().isCreated();
		jdbc.update("update users set active = false where id = ?", c.id());
		assertThat(counselors.canSee(c.id(), idOf(email))).isFalse();
		http.get().uri("/api/students/me/counselors").headers(s::apply).exchange().expectBody()
				.jsonPath("$.content[0].counselorActive").isEqualTo(false);
		link(signedInStudent(), c.code()).expectStatus().isNotFound();
		jdbc.update("update users set active = true where id = ?", c.id());
		assertThat(counselors.canSee(c.id(), idOf(email))).isTrue();
	}

	@Test
	void unlinkedCounselorCannotSeeStudent() {
		long studentId = idOf(createUser(uniqueEmail(), PASSWORD, "STUDENT", false));
		assertThat(counselors.canSee(newCounselor().id(), studentId)).isFalse();
	}

	@Test
	void eleventhFailedAttemptIs429() {
		Session s = signedInStudent();
		for (int i = 0; i < 10; i++) {
			link(s, "ZZZZZZZ" + i).expectStatus().isNotFound();
		}
		link(s, "ZZZZZZZZ").expectStatus().isEqualTo(429);
		link(s, newCounselor().code()).expectStatus().isEqualTo(429);
		link(signedInStudent(), "ZZZZZZZZ").expectStatus().isNotFound(); // other students unaffected
	}

	@Test
	void signupWithValidCodeLinks() {
		Counselor c = newCounselor();
		String email = uniqueEmail();
		signup(email, c.code()).expectStatus().isCreated();
		assertThat(counselors.canSee(c.id(), idOf(email))).isTrue();
	}

	@Test
	void signupWithInvalidCodeIs404AndCreatesNoAccount() {
		String email = uniqueEmail();
		signup(email, "ZZZZZZZZ").expectStatus().isNotFound();
		assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Long.class, email)).isZero();
		signup(email, null).expectStatus().isCreated();
	}

	@Test
	void rotatingCodeInvalidatesOldCodeButKeepsLinks() {
		Counselor c = newCounselor();
		Session cs = signIn(c.email(), PASSWORD);
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		link(signIn(email, PASSWORD), c.code()).expectStatus().isCreated();

		String fresh = (String) http.post().uri("/api/counselor/me/join-code/rotate").headers(cs::apply).exchange()
				.expectStatus().isOk().returnResult(Map.class).getResponseBody().get("joinCode");
		assertThat(fresh).isNotEqualTo(c.code()).hasSize(8);
		link(signedInStudent(), c.code()).expectStatus().isNotFound();
		link(signedInStudent(), fresh).expectStatus().isCreated();
		assertThat(counselors.canSee(c.id(), idOf(email))).isTrue();
		http.get().uri("/api/counselor/me").headers(cs::apply).exchange().expectBody().jsonPath("$.joinCode")
				.isEqualTo(fresh);
	}

	@Test
	void studentEndpointsAreStudentOnlyAndRotateIsCounselorOnly() {
		Session counselor = signIn(newCounselor().email(), PASSWORD);
		http.get().uri("/api/students/me/counselors").headers(counselor::apply).exchange().expectStatus()
				.isForbidden();
		http.post().uri("/api/counselor/me/join-code/rotate").headers(signedInStudent()::apply).exchange()
				.expectStatus().isForbidden();
		http.get().uri("/api/students/me/counselors").exchange().expectStatus().isUnauthorized();
	}

}
