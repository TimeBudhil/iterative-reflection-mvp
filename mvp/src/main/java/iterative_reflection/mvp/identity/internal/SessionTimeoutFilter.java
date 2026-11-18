package iterative_reflection.mvp.identity.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Session timeouts [N2.2]: idle 30 min and absolute 12 h, measured with the injectable {@link Clock} (Spring
 * Session itself reads the system clock, so it only serves as the idle backstop). Runs after Spring Session's
 * filter and before the security chain: an expired session is invalidated here, so the request is anonymous and
 * the normal 401 applies. Sign-in stamps {@link #AUTHENTICATED_AT} and {@link #LAST_SEEN}.
 */
@Component
@Order(-150)
class SessionTimeoutFilter extends OncePerRequestFilter {

	static final String AUTHENTICATED_AT = "app.authenticatedAt";

	static final String LAST_SEEN = "app.lastSeen";

	static final Duration IDLE = Duration.ofMinutes(30);

	static final Duration ABSOLUTE = Duration.ofHours(12);

	private final Clock clock;

	SessionTimeoutFilter(Clock clock) {
		this.clock = clock;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null && session.getAttribute(AUTHENTICATED_AT) instanceof Instant authenticatedAt) {
			Instant now = clock.instant();
			Instant lastSeen = session.getAttribute(LAST_SEEN) instanceof Instant seen ? seen : authenticatedAt;
			if (now.isAfter(authenticatedAt.plus(ABSOLUTE)) || now.isAfter(lastSeen.plus(IDLE))) {
				session.invalidate();
			}
			else {
				session.setAttribute(LAST_SEEN, now);
			}
		}
		chain.doFilter(request, response);
	}

}
