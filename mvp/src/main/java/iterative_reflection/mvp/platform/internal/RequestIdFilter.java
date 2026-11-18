package iterative_reflection.mvp.platform.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns every request a request ID (reusing a well-formed incoming {@code X-Request-Id}), puts it in the MDC as
 * {@code requestId} so every log line carries it, echoes it in the {@code X-Request-Id} response header and writes
 * one access log line per request. Runs before Spring Security so 401/403 responses carry the header too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter extends OncePerRequestFilter {

	static final String HEADER = "X-Request-Id";

	static final String MDC_KEY = "requestId";

	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = requestIdFor(request);
		long start = System.nanoTime();
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		try {
			chain.doFilter(request, response);
		}
		finally {
			log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(),
					(System.nanoTime() - start) / 1_000_000);
			MDC.remove(MDC_KEY);
		}
	}

	private static String requestIdFor(HttpServletRequest request) {
		String incoming = request.getHeader(HEADER);
		return (incoming != null && SAFE_ID.matcher(incoming).matches()) ? incoming : UUID.randomUUID().toString();
	}

}
