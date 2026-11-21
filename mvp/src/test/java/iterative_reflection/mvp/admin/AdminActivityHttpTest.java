package iterative_reflection.mvp.admin;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.counselor.CounselorService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** GET /api/admin/activity and what writes to it [B-R11.6 to B-R11.8, N8]. */
class AdminActivityHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@Autowired
	private CounselorService counselors;

	@BeforeEach
	@AfterEach
	void clean() {
		jdbc.update("delete from admin_activity");
		jdbc.update("delete from app_settings");
	}

	private Session admin() {
		return signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
	}

	private ResponseSpec saveTheme(Session s, String id) {
		return http.put().uri("/api/admin/settings/theme").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("presetId", id)).exchange();
	}

	private ResponseSpec patch(Session s, long id, boolean active) {
		return http.patch().uri("/api/admin/counselors/" + id).headers(s::apply)
				.contentType(MediaType.APPLICATION_JSON).body(Map.of("active", active)).exchange();
	}

	private ResponseSpec create(Session s, String email) {
		return http.post().uri("/api/admin/counselors").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", email, "displayName", "New Counselor", "temporaryPassword", PASSWORD))
				.exchange();
	}

	private long counselorId() {
		String email = createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		counselors.createProfile(id);
		return id;
	}

	@Test
	void eachActionAppearsOnceNewestFirstWithTheAdminName() {
		Session admin = admin();
		saveTheme(admin, "harbor").expectStatus().isOk();
		String email = uniqueEmail();
		create(admin, email).expectStatus().isCreated();
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		patch(admin, id, false).expectStatus().isOk();
		patch(admin, id, true).expectStatus().isOk();

		http.get().uri("/api/admin/activity").headers(admin::apply).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(4)
				.jsonPath("$.content[0].action").isEqualTo("COUNSELOR_REACTIVATED")
				.jsonPath("$.content[0].detail.email").isEqualTo(email)
				.jsonPath("$.content[0].adminName").isEqualTo("Test User")
				.jsonPath("$.content[0].createdAt").isNotEmpty().jsonPath("$.content[0].id").isNumber()
				.jsonPath("$.content[1].action").isEqualTo("COUNSELOR_DEACTIVATED")
				.jsonPath("$.content[1].detail.email").isEqualTo(email)
				.jsonPath("$.content[2].action").isEqualTo("COUNSELOR_CREATED")
				.jsonPath("$.content[2].detail.email").isEqualTo(email)
				.jsonPath("$.content[3].action").isEqualTo("THEME_CHANGED")
				.jsonPath("$.content[3].detail.from").isEqualTo("grove")
				.jsonPath("$.content[3].detail.to").isEqualTo("harbor");
	}

	@Test
	void aNoOpPatchAndASameValueThemeSaveAddNothing() {
		Session admin = admin();
		long id = counselorId();
		patch(admin, id, true).expectStatus().isOk();
		saveTheme(admin, "grove").expectStatus().isOk();
		saveTheme(admin, "plum").expectStatus().isOk();
		saveTheme(admin, "plum").expectStatus().isOk();
		patch(admin, id, false).expectStatus().isOk();
		patch(admin, id, false).expectStatus().isOk();
		http.get().uri("/api/admin/activity").headers(admin::apply).exchange().expectBody()
				.jsonPath("$.page.totalElements").isEqualTo(2).jsonPath("$.content[0].action")
				.isEqualTo("COUNSELOR_DEACTIVATED").jsonPath("$.content[1].action").isEqualTo("THEME_CHANGED");
	}

	@Test
	void aRejectedCreateLeavesNoExtraRow() {
		Session admin = admin();
		String email = uniqueEmail();
		create(admin, email).expectStatus().isCreated();
		create(admin, email).expectStatus().is4xxClientError();
		assertThat(jdbc.queryForObject("select count(*) from admin_activity", Integer.class)).isEqualTo(1);
	}

	@Test
	void rowsArePaginatedNewestFirst() {
		Session admin = admin();
		long id = counselorId();
		// the counselor starts active, so each of the 5 patches flips it: deactivate, reactivate, deactivate, ...
		for (int i = 0; i < 5; i++) {
			patch(admin, id, i % 2 == 1).expectStatus().isOk();
		}
		http.get().uri("/api/admin/activity?size=2&page=0").headers(admin::apply).exchange().expectBody()
				.jsonPath("$.content.length()").isEqualTo(2).jsonPath("$.page.totalElements").isEqualTo(5)
				.jsonPath("$.page.totalPages").isEqualTo(3).jsonPath("$.content[0].action")
				.isEqualTo("COUNSELOR_DEACTIVATED").jsonPath("$.content[1].action").isEqualTo("COUNSELOR_REACTIVATED");
		http.get().uri("/api/admin/activity?size=2&page=2").headers(admin::apply).exchange().expectBody()
				.jsonPath("$.content.length()").isEqualTo(1).jsonPath("$.content[0].action")
				.isEqualTo("COUNSELOR_DEACTIVATED");
	}

	@Test
	void anEmptyLogIsAnEmptyPage() {
		http.get().uri("/api/admin/activity").headers(admin()::apply).exchange().expectStatus().isOk().expectBody()
				.jsonPath("$.content.length()").isEqualTo(0).jsonPath("$.page.totalElements").isEqualTo(0);
	}

	@Test
	void nonAdminsGet403() {
		http.get().uri("/api/admin/activity").headers(signedInStudent()::apply).exchange().expectStatus()
				.isForbidden();
		String email = createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);
		http.get().uri("/api/admin/activity").headers(signIn(email, PASSWORD)::apply).exchange().expectStatus()
				.isForbidden();
	}

}
