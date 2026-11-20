package iterative_reflection.mvp.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import iterative_reflection.mvp.HttpIntegrationTest;
import iterative_reflection.mvp.admin.internal.repository.AdminActivityWriter;
import iterative_reflection.mvp.counselor.CounselorService;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** A failing activity write never fails or undoes the action [B-R11.6]. */
class AdminActivityBestEffortHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	@MockitoBean
	private AdminActivityWriter writer;

	@Autowired
	private CounselorService counselors;

	@BeforeEach
	void writerThrows() {
		doThrow(new IllegalStateException("log table down")).when(writer).insert(anyLong(), anyString(), anyString(),
				any());
	}

	@AfterEach
	void clean() {
		jdbc.update("delete from app_settings");
	}

	private Session admin() {
		return signIn(createUser(uniqueEmail(), PASSWORD, "ADMIN", false), PASSWORD);
	}

	@Test
	void aThemeSaveStillReturns200AndTheValueSticks() {
		http.put().uri("/api/admin/settings/theme").headers(admin()::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("presetId", "slate")).exchange().expectStatus().isOk();
		http.get().uri("/api/settings/theme").exchange().expectBody().jsonPath("$.presetId").isEqualTo("slate");
	}

	@Test
	void aCounselorDeactivationAndCreationStillTakeEffect() {
		Session admin = admin();
		String email = createUser(uniqueEmail(), PASSWORD, "COUNSELOR", false);
		long id = jdbc.queryForObject("select id from users where email = ?", Long.class, email);
		counselors.createProfile(id);
		http.patch().uri("/api/admin/counselors/" + id).headers(admin::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("active", false)).exchange().expectStatus().isOk();
		assertThat(jdbc.queryForObject("select active from users where id = ?", Boolean.class, id)).isFalse();

		String created = uniqueEmail();
		http.post().uri("/api/admin/counselors").headers(admin::apply).contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("email", created, "displayName", "New", "temporaryPassword", PASSWORD)).exchange()
				.expectStatus().isCreated();
		assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Integer.class, created))
				.isEqualTo(1);
	}

}
