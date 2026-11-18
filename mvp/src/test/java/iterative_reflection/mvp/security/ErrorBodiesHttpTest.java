package iterative_reflection.mvp.security;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/** Error responses never leak stack traces, SQL or class names [N2.7], across representative endpoints. */
class ErrorBodiesHttpTest extends HttpIntegrationTest {

	private static final List<String> LEAKS = List.of("Exception", "\tat ", "stackTrace", "org.springframework",
			"org.hibernate", "jakarta.", "java.lang", "iterative_reflection", "select ", "insert ", "SQL", "jdbc:",
			"constraint");

	private void check(String label, HttpMethod method, String uri, Session s, Object body) {
		var spec = http.method(method).uri(uri);
		if (s != null) {
			spec.headers(s::apply);
		}
		if (body != null) {
			spec.contentType(MediaType.APPLICATION_JSON).body(body);
		}
		var result = spec.exchange().returnResult(String.class);
		assertThat(result.getStatus().value()).as(label).isGreaterThanOrEqualTo(400);
		String text = result.getResponseBody();
		if (text != null) {
			for (String leak : LEAKS) {
				assertThat(text).as("%s must not contain '%s'", label, leak).doesNotContain(leak);
			}
		}
	}

	@Test
	void errorBodiesAreFreeOfInternals() {
		Session student = signedInStudent();
		Session admin = signIn(createUser(uniqueEmail(), "correct horse battery", "ADMIN", false),
				"correct horse battery");
		check("malformed json", HttpMethod.POST, "/api/intake", student, "{not json");
		check("bad types", HttpMethod.POST, "/api/careers/1/reflection", student, Map.of("answers", "x", "verdict", 5));
		check("bad path id", HttpMethod.GET, "/api/careers/abc/experience", student, null);
		check("unknown career", HttpMethod.GET, "/api/careers/999999/experience", student, null);
		check("unknown route", HttpMethod.GET, "/api/nope", student, null);
		check("wrong method", HttpMethod.DELETE, "/api/feed/next", student, null);
		check("forbidden", HttpMethod.GET, "/api/admin/audit", student, null);
		check("unauthenticated", HttpMethod.GET, "/api/me", null, null);
		check("weak temporary password", HttpMethod.POST, "/api/admin/counselors", admin,
				Map.of("email", "dup@test.example", "displayName", "x", "temporaryPassword", "short"));
		check("csrf rejected", HttpMethod.POST, "/api/auth/signin", null, Map.of("email", "a@b.c", "password", "x"));
	}

}
