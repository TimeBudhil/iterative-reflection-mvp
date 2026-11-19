package iterative_reflection.mvp.admin;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.counselor.CounselorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** GET /api/admin/audit [R9.6, N8, N2.6]. */
class AdminAuditHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@Autowired
	private CounselorService counselors;

	private Session admin() {
		return signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
	}

	private long studentId() {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		return jdbc.queryForObject("select id from users where email = ?", Long.class, email);
	}

	@Test
	void auditRowsForAStudentAreNewestFirstAndPaginated() {
		String email = createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);
		long cid = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		counselors.createProfile(cid);
		Session counselor = signIn(email, PASSWORD);
		long student = studentId();
		for (int i = 0; i < 3; i++) {
			http.get().uri("/api/counselor/students/{id}", student).headers(counselor::apply).exchange()
					.expectStatus().isForbidden();
		}
		http.get().uri("/api/counselor/students/{id}", studentId()).headers(counselor::apply).exchange();

		http.get().uri("/api/admin/audit?studentId=" + student + "&size=2").headers(admin()::apply).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.content.length()").isEqualTo(2)
				.jsonPath("$.page.totalElements").isEqualTo(3)
				.jsonPath("$.content[0].studentId").isEqualTo(student)
				.jsonPath("$.content[0].counselorId").isEqualTo(cid)
				.jsonPath("$.content[0].outcome").isEqualTo("DENIED")
				.jsonPath("$.content[0].at").exists();
		long newest = jdbc.queryForObject("select max(id) from audit_log where student_id = ?", Long.class,
				student);
		http.get().uri("/api/admin/audit?studentId=" + student).headers(admin()::apply).exchange().expectBody()
				.jsonPath("$.content[0].id").isEqualTo(newest);
	}

	@Test
	void withoutStudentIdReturnsAllRows() {
		http.get().uri("/api/admin/audit").headers(admin()::apply).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.page").exists();
	}

	@Test
	void nonAdminsGet403() {
		http.get().uri("/api/admin/audit").headers(signedInStudent()::apply).exchange().expectStatus()
				.isForbidden();
		Session counselor = signIn(createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false), PASSWORD);
		http.get().uri("/api/admin/audit?studentId=1").headers(counselor::apply).exchange().expectStatus()
				.isForbidden();
	}

}
