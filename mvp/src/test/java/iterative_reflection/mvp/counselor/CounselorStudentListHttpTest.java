package iterative_reflection.mvp.counselor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** GET /api/counselor/students [R8.1, R8.3]. */
class CounselorStudentListHttpTest extends CounselorDashboardTestSupport {

	@Test
	void listsLinkedStudentsWithProgressSummary() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		long alpha = career(0);
		long beta = career(1);
		reflect(s, alpha, 8, "YES");
		view(s, beta, 1);

		list(c, "").expectStatus().isOk().expectBody()
				.jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.content[0].studentId").isEqualTo((int) s.id())
				.jsonPath("$.content[0].displayName").isEqualTo("Test User")
				.jsonPath("$.content[0].careersSeen").isEqualTo(2)
				.jsonPath("$.content[0].reflected").isEqualTo(1)
				.jsonPath("$.content[0].abandoned").isEqualTo(0)
				.jsonPath("$.content[0].yes").isEqualTo(1)
				.jsonPath("$.content[0].maybe").isEqualTo(0)
				.jsonPath("$.content[0].no").isEqualTo(0)
				.jsonPath("$.content[0].intakeCompleted").isEqualTo(false)
				.jsonPath("$.content[0].lastActive").isNotEmpty()
				.jsonPath("$.page.totalElements").isEqualTo(1);
	}

	@Test
	void unlinkedStudentsNeverAppear() {
		Counselor c = newCounselor();
		Counselor other = newCounselor();
		linkedStudent(other);
		newStudent();
		list(c, "").expectStatus().isOk().expectBody().jsonPath("$.content.length()").isEqualTo(0)
				.jsonPath("$.page.totalElements").isEqualTo(0);
	}

	@Test
	void sortedByLastActiveNewestFirstAndPaginated() throws Exception {
		Counselor c = newCounselor();
		Student quiet = linkedStudent(c);
		Student first = linkedStudent(c);
		Student second = linkedStudent(c);
		Student idle = linkedStudent(c);
		view(first, career(0), 1);
		Thread.sleep(20);
		view(second, career(0), 1);
		Thread.sleep(20);
		view(quiet, career(0), 1);

		list(c, "?size=2").expectStatus().isOk().expectBody()
				.jsonPath("$.content[0].studentId").isEqualTo((int) quiet.id())
				.jsonPath("$.content[1].studentId").isEqualTo((int) second.id())
				.jsonPath("$.page.totalElements").isEqualTo(4)
				.jsonPath("$.page.totalPages").isEqualTo(2);
		list(c, "?size=2&page=1").expectStatus().isOk().expectBody()
				.jsonPath("$.content[0].studentId").isEqualTo((int) first.id())
				.jsonPath("$.content[1].studentId").isEqualTo((int) idle.id())
				.jsonPath("$.content[1].lastActive").isEmpty();
	}

	@Test
	void lastActiveIgnoresSignIn() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c); // newStudent signed in
		list(c, "").expectStatus().isOk().expectBody().jsonPath("$.content[0].lastActive").isEmpty();
	}

	@Test
	void lastActiveCoversReflectionAbandonAndIntake() throws Exception {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		view(s, career(0), 1);
		java.time.Instant afterView = lastActive(c);
		Thread.sleep(20);
		http.post().uri("/api/careers/{id}/abandon", career(0)).headers(s.session()::apply).exchange()
				.expectStatus().isNoContent();
		java.time.Instant afterAbandon = lastActive(c);
		assertThat(afterAbandon).isAfter(afterView);
		Thread.sleep(20);
		reflect(s, career(1), 8, "NO");
		java.time.Instant afterReflect = lastActive(c);
		assertThat(afterReflect).isAfter(afterAbandon);
		reflect(s, career(2), 8, "MAYBE");
		reflect(s, career(3), 8, "YES");
		Thread.sleep(20);
		http.post().uri("/api/intake").headers(s.session()::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("answers", Map.of("goals", Map.of("text", "x")))).exchange().expectStatus().isCreated();
		assertThat(lastActive(c)).isAfter(afterReflect);
		list(c, "").expectBody()
				.jsonPath("$.content[0].abandoned").isEqualTo(1)
				.jsonPath("$.content[0].reflected").isEqualTo(3)
				.jsonPath("$.content[0].yes").isEqualTo(1)
				.jsonPath("$.content[0].maybe").isEqualTo(1)
				.jsonPath("$.content[0].no").isEqualTo(1)
				.jsonPath("$.content[0].intakeCompleted").isEqualTo(true);
	}

	@Test
	void revokedStudentLeavesListAndRelinkRestoresIt() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		long linkId = jdbc.queryForObject("select id from counselor_links where student_id = ?", Long.class, s.id());
		http.delete().uri("/api/students/me/counselors/{id}", linkId).headers(s.session()::apply).exchange()
				.expectStatus().isNoContent();
		list(c, "").expectBody().jsonPath("$.content.length()").isEqualTo(0);
		counselors.link(s.id(), c.code());
		list(c, "").expectBody().jsonPath("$.content.length()").isEqualTo(1);
	}

	private java.time.Instant lastActive(Counselor c) {
		String v = (String) ((java.util.List<Map<String, Object>>) list(c, "").expectStatus().isOk()
				.returnResult(Map.class).getResponseBody().get("content")).get(0).get("lastActive");
		return java.time.Instant.parse(v);
	}

}
