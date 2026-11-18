package iterative_reflection.mvp.security;

import static org.assertj.core.api.Assertions.assertThat;

import iterative_reflection.mvp.HttpIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Inventory-driven authorization sweep [N2.3, N2.6]: every registered {@code /api/**} route is enumerated from the
 * handler mapping, so a route added later is covered without touching this class. The expected access table below
 * is written from the spec, independently of SecurityConfig.
 */
class RouteInventoryHttpTest extends HttpIntegrationTest {

	private static final String PASSWORD = "correct horse battery";

	record Route(HttpMethod method, String pattern) {
		String uri() {
			return pattern.replaceAll("\\{[^}]+}", "1");
		}

		@Override
		public String toString() {
			return method + " " + pattern;
		}
	}

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping mappings;

	List<Route> routes() {
		List<Route> routes = new ArrayList<>();
		for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
			if (info.getPathPatternsCondition() == null) {
				continue;
			}
			for (var pattern : info.getPathPatternsCondition().getPatterns()) {
				if (!pattern.getPatternString().startsWith("/api/")) {
					continue;
				}
				Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
				assertThat(methods).as("%s must declare its HTTP method", pattern).isNotEmpty();
				for (RequestMethod m : methods) {
					routes.add(new Route(HttpMethod.valueOf(m.name()), pattern.getPatternString()));
				}
			}
		}
		// signout is a filter, not a handler, but it is a state-changing route all the same
		routes.add(new Route(HttpMethod.POST, "/api/auth/signout"));
		assertThat(routes).hasSizeGreaterThan(20);
		return routes;
	}

	static boolean isPublic(Route r) {
		return r.method() == HttpMethod.POST && Set.of("/api/auth/signup", "/api/auth/signin").contains(r.pattern());
	}

	/** Roles that may call the route, per the spec; null means "any signed-in role". */
	static Set<String> rolesFor(Route r) {
		String p = r.pattern();
		if (p.startsWith("/api/admin/")) {
			return Set.of("ADMIN");
		}
		if (p.startsWith("/api/counselor/")) {
			return Set.of("COUNSELOR");
		}
		if (p.equals("/api/me") || p.equals("/api/me/password") || p.equals("/api/auth/signout")) {
			return null;
		}
		// the shared read-only catalog/question reads are open to every role
		if (r.method() == HttpMethod.GET
				&& (p.equals("/api/reflection-questions") || p.equals("/api/careers/{id}/experience"))) {
			return null;
		}
		// everything else reads or writes a student's own data
		return Set.of("STUDENT");
	}

	private Session sessionFor(String role) {
		return signIn(createUser(uniqueEmail(), PASSWORD, role, false), PASSWORD);
	}

	private int call(Route r, Consumer<HttpHeaders> headers) {
		var spec = http.method(r.method()).uri(r.uri());
		spec.headers(headers);
		if (r.method() != HttpMethod.GET) {
			spec.contentType(MediaType.APPLICATION_JSON).body("{}");
		}
		return spec.exchange().returnResult().getStatus().value();
	}

	@Test
	void everyStateChangingRouteRejectsMissingOrWrongCsrfWith403() {
		Session student = sessionFor("STUDENT");
		Session counselor = sessionFor("COUNSELOR");
		Session admin = sessionFor("ADMIN");
		List<String> failures = new ArrayList<>();
		for (Route r : routes()) {
			if (r.method() == HttpMethod.GET) {
				continue;
			}
			Set<String> roles = rolesFor(r);
			Session s = roles == null || roles.contains("STUDENT") ? student
					: roles.contains("COUNSELOR") ? counselor : admin;
			// authenticated, right role, cookie only: no CSRF header
			int noHeader = call(r, h -> h.set("Cookie", "SESSION=" + s.sessionId()));
			// authenticated, header does not match the cookie
			int mismatch = call(r, h -> {
				h.set("Cookie", "SESSION=" + s.sessionId() + "; XSRF-TOKEN=" + s.csrfToken());
				h.set("X-XSRF-TOKEN", "forged");
			});
			// anonymous with no token (covers signup/signin)
			int anonymous = call(r, h -> {
			});
			if (noHeader != 403 || mismatch != 403 || anonymous != 403) {
				failures.add(r + " -> " + noHeader + "/" + mismatch + "/" + anonymous);
			}
		}
		assertThat(failures).isEmpty();
	}

	@Test
	void everyRouteEnforcesItsRoleRule() {
		Map<String, Session> sessions = Map.of("STUDENT", sessionFor("STUDENT"), "COUNSELOR", sessionFor("COUNSELOR"),
				"ADMIN", sessionFor("ADMIN"));
		List<String> failures = new ArrayList<>();
		for (Route r : routes()) {
			if (r.pattern().equals("/api/auth/signout")) {
				continue; // would end the session used below
			}
			Csrf csrf = csrf();
			int anonymous = call(r, csrf::apply);
			if (!isPublic(r) && anonymous != 401) {
				failures.add(r + " anonymous -> " + anonymous + " (expected 401)");
			}
			Set<String> allowed = rolesFor(r);
			sessions.forEach((role, session) -> {
				int status = call(r, session::apply);
				boolean mayCall = isPublic(r) || allowed == null || allowed.contains(role);
				if (!mayCall && status != 403) {
					failures.add(r + " as " + role + " -> " + status + " (expected 403)");
				}
				if (mayCall && !isPublic(r) && status == 401) {
					failures.add(r + " as " + role + " -> 401 for a signed-in user");
				}
			});
		}
		assertThat(failures).isEmpty();
	}

	@Test
	void whileMustChangePasswordOnlyMeAndPasswordChangeAreReachable() {
		Session s = signIn(createUser(uniqueEmail(), PASSWORD, "COUNSELOR", true), PASSWORD);
		List<String> failures = new ArrayList<>();
		for (Route r : routes()) {
			String p = r.pattern();
			boolean open = isPublic(r) || p.equals("/api/auth/signout")
					|| (r.method() == HttpMethod.GET && p.equals("/api/me"))
					|| (r.method() == HttpMethod.PUT && p.equals("/api/me/password"));
			if (open) {
				continue;
			}
			int status = call(r, s::apply);
			if (status != 403) {
				failures.add(r + " -> " + status + " (expected 403)");
			}
		}
		assertThat(failures).isEmpty();
	}

}
