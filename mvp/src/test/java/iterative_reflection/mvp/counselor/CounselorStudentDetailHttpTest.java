package iterative_reflection.mvp.counselor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

/** GET /api/counselor/students/{id} and its audit trail [R8.2, R8.3, R8.6, R2.4, R2.8]. */
class CounselorStudentDetailHttpTest extends CounselorDashboardTestSupport {

	@Autowired
	private CounselorService service;

	private List<AuditEntry> audit(long studentId) {
		return service.auditLog(studentId, PageRequest.of(0, 100)).getContent();
	}

	@Test
	void detailHasReflectionsAbandonsAffinitiesAndIntake() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		long reflected = career(0);
		long abandoned = career(1);
		reflect(s, reflected, 9, "YES");
		view(s, abandoned, 1);
		http.post().uri("/api/careers/{id}/abandon", abandoned).headers(s.session()::apply).exchange()
				.expectStatus().isNoContent();
		reflect(s, career(2), 5, "NO");
		reflect(s, career(3), 5, "NO");
		http.post().uri("/api/intake").headers(s.session()::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("answers", Map.of("goals", Map.of("text", "be a nurse")))).exchange().expectStatus()
				.isCreated();

		detail(c, s.id()).expectStatus().isOk().expectBody()
				.jsonPath("$.studentId").isEqualTo((int) s.id())
				.jsonPath("$.displayName").isEqualTo("Test User")
				.jsonPath("$.reflections.length()").isEqualTo(3)
				.jsonPath("$.reflections[?(@.careerId==" + reflected + ")].verdict").isEqualTo("YES")
				.jsonPath("$.reflections[?(@.careerId==" + reflected + ")].answers.length()").isEqualTo(5)
				.jsonPath("$.reflections[?(@.careerId==" + reflected + ")].answers[0].score").isEqualTo(9)
				.jsonPath("$.reflections[?(@.careerId==" + reflected + ")].answers[0].openText")
				.isEqualTo("because interest")
				.jsonPath("$.abandonedCareers.length()").isEqualTo(1)
				.jsonPath("$.abandonedCareers[0].careerId").isEqualTo((int) abandoned)
				.jsonPath("$.abandonedCareers[0].lastViewedPage").isEqualTo(1)
				.jsonPath("$.topAffinities.length()").isNotEmpty()
				.jsonPath("$.intake.answers[0].key").isEqualTo("goals")
				.jsonPath("$.intake.answers[0].text").isEqualTo("be a nurse");
	}

	@Test
	void detailLimitsAffinitiesToFive() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		for (int i = 0; i < 4; i++) {
			reflect(s, career(i), 9, "YES");
		}
		detail(c, s.id()).expectBody().jsonPath("$.topAffinities.length()").isEqualTo(5)
				.jsonPath("$.intake").isEmpty();
	}

	@Test
	void historyFromBeforeTheLinkIsVisible() {
		Counselor c = newCounselor();
		Student s = newStudent();
		reflect(s, career(0), 7, "MAYBE");
		counselors.link(s.id(), c.code());
		detail(c, s.id()).expectStatus().isOk().expectBody().jsonPath("$.reflections.length()").isEqualTo(1);
	}

	@Test
	void unlinkedStudentIs403AndAudited() {
		Counselor c = newCounselor();
		Student stranger = newStudent();
		detail(c, stranger.id()).expectStatus().isForbidden();
		assertThat(audit(stranger.id())).singleElement().satisfies(e -> {
			assertThat(e.counselorId()).isEqualTo(c.id());
			assertThat(e.outcome()).isEqualTo("DENIED");
			assertThat(e.at()).isNotNull();
		});
	}

	@Test
	void unknownStudentIs403NotFound() {
		Counselor c = newCounselor();
		detail(c, 987654321L).expectStatus().isForbidden();
		assertThat(audit(987654321L)).hasSize(1);
	}

	@Test
	void allowedReadIsAudited() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		detail(c, s.id()).expectStatus().isOk();
		detail(c, s.id()).expectStatus().isOk();
		assertThat(audit(s.id())).hasSize(2).allSatisfy(e -> {
			assertThat(e.outcome()).isEqualTo("ALLOWED");
			assertThat(e.counselorId()).isEqualTo(c.id());
		});
	}

	@Test
	void revokeBlocksAndRelinkRestoresAccess() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		reflect(s, career(0), 7, "YES");
		long linkId = jdbc.queryForObject("select id from counselor_links where student_id = ?", Long.class, s.id());
		http.delete().uri("/api/students/me/counselors/{id}", linkId).headers(s.session()::apply).exchange()
				.expectStatus().isNoContent();
		detail(c, s.id()).expectStatus().isForbidden();
		counselors.link(s.id(), c.code());
		detail(c, s.id()).expectStatus().isOk().expectBody().jsonPath("$.reflections.length()").isEqualTo(1);
		assertThat(audit(s.id())).extracting(AuditEntry::outcome).containsExactly("ALLOWED", "DENIED");
	}

	@Test
	void anotherCounselorsStudentIs403() {
		Counselor owner = newCounselor();
		Counselor other = newCounselor();
		Student s = linkedStudent(owner);
		detail(other, s.id()).expectStatus().isForbidden();
	}

	@Test
	void studentsAndAnonymousCannotReadDetail() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		http.get().uri("/api/counselor/students/{id}", s.id()).headers(s.session()::apply).exchange()
				.expectStatus().isForbidden();
		http.get().uri("/api/counselor/students/{id}", s.id()).exchange().expectStatus().isUnauthorized();
		assertThat(audit(s.id())).isEmpty();
	}

}
