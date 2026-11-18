package iterative_reflection.mvp.feed.internal.integration;

import iterative_reflection.mvp.career.CareerCatalog;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.identity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import iterative_reflection.mvp.reflection.ReflectionReader;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enforces R4.2 where it cannot create a module cycle (career and reflection must not depend on feed): interceptors on
 * the career endpoints a student uses to open, progress through, abandon or reflect on a career. A student may only act
 * on the career that is currently their in-progress feed card; anything else, including a career never served or one
 * whose card was cleared (e.g. by abandoning), gives 409. Reading an experience or editing a reflection is also
 * allowed for careers the student already reflected on (review and edit).
 */
@Configuration
public class InProgressGuard implements WebMvcConfigurer {

	private final FeedService feed;

	private final ReflectionReader reflections;

	private final CareerCatalog catalog;

	InProgressGuard(FeedService feed, ReflectionReader reflections, CareerCatalog catalog) {
		this.catalog = catalog;
		this.feed = feed;
		this.reflections = reflections;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(guard(false)).addPathPatterns("/api/careers/{id}/pages/{n}/viewed",
				"/api/careers/{id}/abandon", "/api/careers/{careerId}/reflection");
		registry.addInterceptor(guard(true)).addPathPatterns("/api/careers/{id}/experience",
				"/api/careers/{careerId}/reflection/versions");
	}

	private HandlerInterceptor guard(boolean allowReflected) {
		return new HandlerInterceptor() {
			@Override
			@SuppressWarnings("unchecked")
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal user)
						|| user.role() != Role.STUDENT) {
					return true;
				}
				Map<String, String> vars = (Map<String, String>) request
						.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
				String idVar = vars == null ? null : vars.getOrDefault("id", vars.get("careerId"));
				if (idVar == null) {
					return true;
				}
				long requested;
				try {
					requested = Long.parseLong(idVar);
				}
				catch (NumberFormatException e) {
					return true;
				}
				if (catalog.findCareer(requested).filter(CareerSummary::active).isEmpty()) {
					return true; // unknown or retired: the controller answers 404
				}
				// a reflection can be edited after submitting; only the first submit needs the in-progress card
				boolean reflectionEdit = request.getMethod().equals("PUT");
				OptionalLong current = feed.inProgressCareerId(user.id());
				if (current.isPresent() && current.getAsLong() == requested) {
					return true;
				}
				if ((allowReflected || reflectionEdit) && reflections.reflectedCareerIds(user.id()).contains(requested)) {
					return true;
				}
				throw new ResponseStatusException(HttpStatus.CONFLICT,
						"Finish your current career before starting another.");
			}
		};
	}

}
