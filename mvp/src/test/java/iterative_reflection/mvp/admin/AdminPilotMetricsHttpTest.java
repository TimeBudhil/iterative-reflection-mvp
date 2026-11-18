package iterative_reflection.mvp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/** GET /api/admin/pilot-metrics and the matching Prometheus gauges [R9.5, N5.3, N2.6]. */
@TestPropertySource(properties = "pilot.metrics.cache-ttl=PT0S")
class AdminPilotMetricsHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	private Session admin() {
		return signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
	}

	private record Student(long id, Session session) {
	}

	private Student student(String signupDate) {
		String email = createUser(uniqueEmail(), PASSWORD, "STUDENT", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		Session s = signIn(email, PASSWORD);
		jdbc.update("update users set created_at = ? where id = ?",
				java.sql.Timestamp.from(java.time.Instant.parse(signupDate + "T12:00:00Z")), id);
		return new Student(id, s);
	}

	private List<Long> careers() {
		return jdbc.queryForList("select id from careers where active = true order by default_order, id limit 3",
				Long.class);
	}

	private void reflect(Student s, long career, String verdict) {
		reflect(s.session(), career, 8, verdict);
	}

	private void reflectThree(Student s, String v1, String v2, String v3) {
		List<Long> c = careers();
		reflect(s, c.get(0), v1);
		reflect(s, c.get(1), v2);
		reflect(s, c.get(2), v3);
	}

	private void intake(Student s) {
		long tag = jdbc.queryForObject("select min(id) from tags", Long.class);
		http.post().uri("/api/intake").headers(s.session()::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("answers", Map.of("interests", Map.of("tagIds", List.of(tag))))).exchange()
				.expectStatus().isCreated();
	}

	@Test
	void countsOnlyActiveNonDemoStudentsInTheSignupWindow() {
		Student a = student("2001-05-10");
		reflectThree(a, "YES", "MAYBE", "NO");
		intake(a);
		Student b = student("2001-05-31");
		reflect(b, careers().get(0), "YES");
		student("2001-05-01");
		Student inactive = student("2001-05-15");
		reflectThree(inactive, "YES", "YES", "YES");
		jdbc.update("update users set active = false where id = ?", inactive.id());
		Student demo = student("2001-05-16");
		reflectThree(demo, "NO", "NO", "NO");
		jdbc.update("update users set demo = true where id = ?", demo.id());
		Student outside = student("2001-06-01");
		reflectThree(outside, "NO", "NO", "NO");
		Student before = student("2001-04-30");
		reflectThree(before, "NO", "NO", "NO");
		createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);

		http.get().uri("/api/admin/pilot-metrics?from=2001-05-01&to=2001-05-31").headers(admin()::apply).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.totalStudents").isEqualTo(3)
				.jsonPath("$.studentsWithThreeReflections").isEqualTo(1)
				.jsonPath("$.percentWithThreeReflections").isEqualTo(33.33)
				.jsonPath("$.studentsWithIntake").isEqualTo(1)
				.jsonPath("$.percentIntakeCompleted").isEqualTo(33.33)
				.jsonPath("$.verdicts.YES").isEqualTo(2)
				.jsonPath("$.verdicts.MAYBE").isEqualTo(1)
				.jsonPath("$.verdicts.NO").isEqualTo(1);
	}

	@Test
	void emptyWindowGivesZerosNotErrors() {
		http.get().uri("/api/admin/pilot-metrics?from=1999-01-01&to=1999-01-31").headers(admin()::apply).exchange()
				.expectStatus().isOk().expectBody()
				.jsonPath("$.totalStudents").isEqualTo(0)
				.jsonPath("$.percentWithThreeReflections").isEqualTo(0)
				.jsonPath("$.percentIntakeCompleted").isEqualTo(0)
				.jsonPath("$.verdicts.YES").isEqualTo(0);
	}

	@Test
	void badDateIs400() {
		http.get().uri("/api/admin/pilot-metrics?from=nope").headers(admin()::apply).exchange().expectStatus()
				.isBadRequest();
	}

	@Test
	void allTimeIsTheDefault() {
		Session admin = admin();
		long before = total(admin);
		student("2002-01-01");
		assertThat(total(admin)).isEqualTo(before + 1);
	}

	private long total(Session admin) {
		return ((Number) http.get().uri("/api/admin/pilot-metrics").headers(admin::apply).exchange()
				.expectStatus().isOk().returnResult(Map.class).getResponseBody().get("totalStudents")).longValue();
	}

	@Test
	void nonAdminsGet403() {
		http.get().uri("/api/admin/pilot-metrics").headers(signedInStudent()::apply).exchange().expectStatus()
				.isForbidden();
		Session counselor = signIn(createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false), PASSWORD);
		http.get().uri("/api/admin/pilot-metrics").headers(counselor::apply).exchange().expectStatus()
				.isForbidden();
	}

	private double gauge(Session admin, String name) {
		String text = http.get().uri("/actuator/prometheus").headers(admin::apply).exchange().expectStatus().isOk()
				.returnResult(String.class).getResponseBody();
		Matcher m = Pattern.compile("^" + Pattern.quote(name) + " ([0-9.eE+-]+)$", Pattern.MULTILINE).matcher(text);
		assertThat(m.find()).as("gauge " + name).isTrue();
		return Double.parseDouble(m.group(1));
	}

	@Test
	void prometheusGaugesTrackTheAllTimeMetrics() {
		Session admin = admin();
		double students = gauge(admin, "pilot_students");
		double yes = gauge(admin, "pilot_verdicts{verdict=\"YES\"}");
		double withThree = gauge(admin, "pilot_students_with_three_reflections");
		Student s = student("2003-01-01");
		reflectThree(s, "YES", "YES", "NO");
		intake(s);
		Student demo = student("2003-01-02");
		jdbc.update("update users set demo = true where id = ?", demo.id());
		reflect(demo, careers().get(0), "YES");

		assertThat(gauge(admin, "pilot_students")).isEqualTo(students + 1);
		assertThat(gauge(admin, "pilot_verdicts{verdict=\"YES\"}")).isEqualTo(yes + 2);
		assertThat(gauge(admin, "pilot_students_with_three_reflections")).isEqualTo(withThree + 1);
		assertThat(gauge(admin, "pilot_students_with_intake")).isGreaterThanOrEqualTo(1);
		assertThat(gauge(admin, "pilot_percent_with_three_reflections")).isBetween(0.0, 100.0);
		assertThat(gauge(admin, "pilot_percent_intake_completed")).isBetween(0.0, 100.0);
		assertThat(gauge(admin, "pilot_verdicts{verdict=\"MAYBE\"}")).isGreaterThanOrEqualTo(0);
		assertThat(gauge(admin, "pilot_verdicts{verdict=\"NO\"}")).isGreaterThanOrEqualTo(1);
	}

}
