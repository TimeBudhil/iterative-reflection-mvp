package iterative_reflection.mvp.feed.internal;

import iterative_reflection.mvp.feed.FeedService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.identity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 * Enforces R4.2 where it cannot create a module cycle (career must not depend on feed): an interceptor on the
 * career endpoints a student uses to open or progress through a career. Opening a career other than the
 * in-progress one gives 409. Ticket 12's abandon endpoint should be added to the path patterns.
 */
@Configuration
class InProgressGuard implements WebMvcConfigurer {

	private final FeedService feed;

	InProgressGuard(FeedService feed) {
		this.feed = feed;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new HandlerInterceptor() {
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
				if (vars == null || !vars.containsKey("id")) {
					return true;
				}
				long requested;
				try {
					requested = Long.parseLong(vars.get("id"));
				}
				catch (NumberFormatException e) {
					return true;
				}
				OptionalLong current = feed.inProgressCareerId(user.id());
				if (current.isPresent() && current.getAsLong() != requested) {
					throw new ResponseStatusException(HttpStatus.CONFLICT,
							"Finish your current career before starting another.");
				}
				return true;
			}
		}).addPathPatterns("/api/careers/{id}/experience", "/api/careers/{id}/pages/{n}/viewed");
	}

}
