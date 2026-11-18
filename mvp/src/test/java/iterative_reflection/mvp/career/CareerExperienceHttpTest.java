package iterative_reflection.mvp.career;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

class CareerExperienceHttpTest extends HttpIntegrationTest {

	@Autowired
	CareerCatalog catalog;

	long careerId(String title) {
		return jdbc.queryForObject("select id from careers where title = ?", Long.class, title);
	}

	ResponseSpec experience(Session s, long id) {
		return http.get().uri("/api/careers/{id}/experience", id).headers(s::apply).exchange();
	}

	ResponseSpec viewed(Session s, long id, int page) {
		return http.post().uri("/api/careers/{id}/pages/{n}/viewed", id, page).headers(s::apply).exchange();
	}

	@Test
	void experienceRequiresSignIn() {
		http.get().uri("/api/careers/{id}/experience", careerId("Fixture Career Alpha")).exchange()
				.expectStatus().isUnauthorized();
	}

	@Test
	void experienceReturnsCareerFieldsAndOrderedTypedBlocks() {
		Session s = signedInStudent();
		experience(s, careerId("Fixture Career Alpha")).expectStatus().isOk().expectBody()
				.jsonPath("$.title").isEqualTo("Fixture Career Alpha")
				.jsonPath("$.estimatedMinutes").isEqualTo(7)
				.jsonPath("$.summary").isNotEmpty()
				.jsonPath("$.tags.length()").isEqualTo(2)
				.jsonPath("$.tags[?(@.name=='Fixture technology')].category").isEqualTo("Fixture cluster")
				.jsonPath("$.lastViewedPage").isEqualTo(0)
				.jsonPath("$.lastPage").isEqualTo(3)
				.jsonPath("$.pages.length()").isEqualTo(3)
				.jsonPath("$.pages[0].number").isEqualTo(1)
				.jsonPath("$.pages[0].blocks[0].type").isEqualTo("TEXT")
				.jsonPath("$.pages[0].blocks[0].text").isEqualTo("Welcome to the job.")
				.jsonPath("$.pages[0].blocks[1].type").isEqualTo("IMAGE_URL")
				.jsonPath("$.pages[1].blocks[0].type").isEqualTo("VIDEO_EMBED")
				.jsonPath("$.pages[1].blocks[0].url").isEqualTo("https://www.youtube.com/embed/abc123")
				.jsonPath("$.pages[1].blocks[1].type").isEqualTo("AUDIO_EMBED")
				.jsonPath("$.pages[2].blocks[0].type").isEqualTo("EXTERNAL_LINK")
				.jsonPath("$.pages[2].blocks[0].label").isEqualTo("Occupational Outlook Handbook");
	}

	@Test
	void unknownAndRetiredCareersAre404() {
		Session s = signedInStudent();
		experience(s, 999_999L).expectStatus().isNotFound();
		experience(s, careerId("Fixture Career Retired")).expectStatus().isNotFound();
	}

	@Test
	void viewingRecordsProgressAndReopeningReturnsIt() {
		Session s = signedInStudent();
		long id = careerId("Fixture Career Alpha");
		viewed(s, id, 2).expectStatus().isOk().expectBody().jsonPath("$.lastViewedPage").isEqualTo(2);
		experience(s, id).expectStatus().isOk().expectBody().jsonPath("$.lastViewedPage").isEqualTo(2);
		// going back never lowers progress
		viewed(s, id, 1).expectStatus().isOk().expectBody().jsonPath("$.lastViewedPage").isEqualTo(2);
		viewed(s, id, 3).expectStatus().isOk();
		experience(s, id).expectBody().jsonPath("$.lastViewedPage").isEqualTo(3);
	}

	@Test
	void progressIsPerStudent() {
		long id = careerId("Fixture Career Alpha");
		Session a = signedInStudent();
		Session b = signedInStudent();
		viewed(a, id, 2).expectStatus().isOk();
		experience(b, id).expectBody().jsonPath("$.lastViewedPage").isEqualTo(0);
	}

	@Test
	void pageOutsideTheCareerIs404() {
		Session s = signedInStudent();
		long id = careerId("Fixture Career Beta");
		viewed(s, id, 0).expectStatus().isNotFound();
		viewed(s, id, 3).expectStatus().isNotFound();
		viewed(s, careerId("Fixture Career Retired"), 1).expectStatus().isNotFound();
	}

	@Test
	void viewedRequiresCsrf() {
		Session s = signedInStudent();
		http.post().uri("/api/careers/{id}/pages/1/viewed", careerId("Fixture Career Beta"))
				.header("Cookie", "SESSION=" + s.sessionId()).exchange().expectStatus().isForbidden();
	}

	@Test
	void onlyStudentsRecordProgress() {
		Session c = signIn(createUser(uniqueEmail(), "correct horse battery", "COUNSELOR", false),
				"correct horse battery");
		viewed(c, careerId("Fixture Career Beta"), 1).expectStatus().isForbidden();
	}

	@Test
	void lastViewedPageIsClampedWhenContentShrinks() {
		Session s = signedInStudent();
		long id = careerId("Fixture Career Alpha");
		viewed(s, id, 3).expectStatus().isOk();
		long studentId = jdbc.queryForObject("select max(student_id) from page_progress where career_id = ?",
				Long.class, id);
		// simulate a migration that lowers the page count below the stored progress
		jdbc.update("update page_progress set last_viewed_page = 99 where student_id = ? and career_id = ?",
				studentId, id);
		experience(s, id).expectBody().jsonPath("$.lastViewedPage").isEqualTo(3);
		assertThat(catalog.lastViewedPage(studentId, id)).isEqualTo(3);
	}

	@Test
	void publicApiExposesSummaryTagsLastPageAndActiveCareersInDefaultOrder() {
		long alpha = careerId("Fixture Career Alpha");
		var summary = catalog.findCareer(alpha).orElseThrow();
		assertThat(summary.title()).isEqualTo("Fixture Career Alpha");
		assertThat(summary.lastPage()).isEqualTo(3);
		assertThat(summary.active()).isTrue();
		assertThat(summary.tags()).extracting(t -> t.name()).containsExactlyInAnyOrder("Fixture technology",
				"Fixture hands-on");
		assertThat(catalog.lastPage(alpha)).isEqualTo(3);

		List<String> titles = catalog.activeCareersInDefaultOrder().stream().map(c -> c.title())
				.filter(t -> t.startsWith("Fixture")).toList();
		assertThat(titles).containsExactly("Fixture Career Alpha", "Fixture Career Beta");
		assertThat(catalog.findCareer(careerId("Fixture Career Retired")).orElseThrow().active()).isFalse();
	}

	@Test
	void noCareerWriteEndpointsExist() {
		Session s = signedInStudent();
		long id = careerId("Fixture Career Beta");
		for (HttpMethod m : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
			http.method(m).uri("/api/careers").headers(s::apply).contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("title", "x")).exchange().expectStatus().is4xxClientError();
			http.method(m).uri("/api/careers/{id}", id).headers(s::apply).exchange().expectStatus()
					.is4xxClientError();
		}
		assertThat(jdbc.queryForObject("select count(*) from careers where title = 'x'", Integer.class)).isZero();
	}

}
