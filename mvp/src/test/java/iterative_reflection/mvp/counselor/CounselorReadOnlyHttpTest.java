package iterative_reflection.mvp.counselor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** The counselor dashboard is strictly read-only [R8.4]. */
class CounselorReadOnlyHttpTest extends CounselorDashboardTestSupport {

	/** The only state-changing counselor routes (the user's own password change lives under /api/me). */
	private static final Set<String> ALLOWLIST = Set.of("POST /api/counselor/me/join-code/rotate");

	@Autowired
	@org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping mappings;

	@Test
	void everyCounselorRouteIsGetExceptTheAllowlist() {
		List<String> violations = new ArrayList<>();
		for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
			if (info.getPathPatternsCondition() == null) {
				continue;
			}
			for (var pattern : info.getPathPatternsCondition().getPatterns()) {
				if (!pattern.getPatternString().startsWith("/api/counselor")) {
					continue;
				}
				Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
				if (methods.isEmpty()) {
					violations.add("ANY " + pattern);
				}
				for (RequestMethod m : methods) {
					String route = m + " " + pattern;
					if (m != RequestMethod.GET && !ALLOWLIST.contains(route)) {
						violations.add(route);
					}
				}
			}
		}
		assertThat(violations).isEmpty();
	}

	@Test
	void counselorWritesToDashboardRoutesAreForbidden() {
		Counselor c = newCounselor();
		Student s = linkedStudent(c);
		for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
			for (String uri : List.of("/api/counselor/students", "/api/counselor/students/" + s.id(),
					"/api/counselor/students/" + s.id() + "/anything", "/api/counselor/me")) {
				http.method(method).uri(uri).headers(c.session()::apply).exchange().expectStatus().isForbidden();
			}
		}
		// the allowlisted rotation still works
		http.post().uri("/api/counselor/me/join-code/rotate").headers(c.session()::apply).exchange().expectStatus()
				.isOk();
	}

}
