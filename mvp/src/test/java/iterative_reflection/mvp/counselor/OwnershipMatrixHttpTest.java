package iterative_reflection.mvp.counselor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Cross-slice ownership guarantees [N2.6]: who can read whose data. */
class OwnershipMatrixHttpTest extends CounselorDashboardTestSupport {

	@Test
	void studentCannotSeeAnotherStudentsReflectionsOrVersions() {
		Student a = newStudent();
		Student b = newStudent();
		long career = career(0);
		reflect(b, career, 9, "YES");
		http.get().uri("/api/students/me/reflections").headers(a.session()::apply).exchange().expectStatus().isOk()
				.expectBody().jsonPath("$.content.length()").isEqualTo(0);
		// A never served nor reflected on that career, so there is nothing to read, whatever B wrote
		int status = http.get().uri("/api/careers/{id}/reflection/versions", career).headers(a.session()::apply)
				.exchange().returnResult().getStatus().value();
		assertThat(status).isEqualTo(409);
		// and B's own data is untouched
		http.get().uri("/api/students/me/reflections").headers(b.session()::apply).exchange().expectBody()
				.jsonPath("$.content.length()").isEqualTo(1);
	}

	@Test
	void studentCannotRevokeAnotherStudentsCounselorLink() {
		Counselor c = newCounselor();
		Student owner = linkedStudent(c);
		Student attacker = newStudent();
		long linkId = jdbc.queryForObject("select id from counselor_links where student_id = ?", Long.class,
				owner.id());
		int status = http.delete().uri("/api/students/me/counselors/{id}", linkId).headers(attacker.session()::apply)
				.exchange().returnResult().getStatus().value();
		assertThat(status).isIn(403, 404);
		detail(c, owner.id()).expectStatus().isOk();
		http.get().uri("/api/students/me/counselors").headers(attacker.session()::apply).exchange().expectBody()
				.jsonPath("$.content.length()").isEqualTo(0);
	}

	@Test
	void counselorSeesOnlyActivelyLinkedStudents() {
		Counselor c = newCounselor();
		Student linked = linkedStudent(c);
		Student revoked = linkedStudent(c);
		Student unlinked = newStudent();
		Student ofOther = linkedStudent(newCounselor());
		long linkId = jdbc.queryForObject("select id from counselor_links where student_id = ?", Long.class,
				revoked.id());
		http.delete().uri("/api/students/me/counselors/{id}", linkId).headers(revoked.session()::apply).exchange()
				.expectStatus().isNoContent();

		list(c, "").expectStatus().isOk().expectBody().jsonPath("$.content.length()").isEqualTo(1)
				.jsonPath("$.content[0].studentId").isEqualTo(linked.id());
		detail(c, linked.id()).expectStatus().isOk();
		for (long id : List.of(revoked.id(), unlinked.id(), ofOther.id(), 999_999L)) {
			detail(c, id).expectStatus().isForbidden();
		}
	}

}
