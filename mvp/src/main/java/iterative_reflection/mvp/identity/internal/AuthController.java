package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Signup and sign-in. Sign-out is the security chain's logout ({@code POST /api/auth/signout}, 204). */
@RestController
class AuthController {

	private final UserAccounts accounts;

	private final AuthenticationManager authenticationManager;

	private final LoginRateLimiter rateLimiter;

	private final Clock clock;

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	/** Session-fixation protection: new session id, and rotate the CSRF token, on every sign-in. */
	private final SessionAuthenticationStrategy onSignIn = new CompositeSessionAuthenticationStrategy(List.of(
			new ChangeSessionIdAuthenticationStrategy(),
			new CsrfAuthenticationStrategy(CookieCsrfTokenRepository.withHttpOnlyFalse())));

	private final MeResponses responses;

	AuthController(UserAccounts accounts, AuthenticationManager authenticationManager, LoginRateLimiter rateLimiter,
			Clock clock, MeResponses responses) {
		this.responses = responses;
		this.rateLimiter = rateLimiter;
		this.clock = clock;
		this.accounts = accounts;
		this.authenticationManager = authenticationManager;
	}

	@PostMapping("/api/auth/signup")
	@ResponseStatus(HttpStatus.CREATED)
	MeResponse signUp(@Valid @RequestBody SignupRequest request) {
		return responses.of(accounts.signUpStudent(request));
	}

	@PostMapping("/api/auth/signin")
	MeResponse signIn(@Valid @RequestBody SignInRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		String email = UserAccounts.normalizeEmail(body.email());
		String ip = request.getRemoteAddr();
		if (rateLimiter.isBlocked(email, ip)) {
			throw new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, ProblemDetail.forStatusAndDetail(
					HttpStatus.TOO_MANY_REQUESTS, "Too many failed sign-in attempts. Try again later."), null);
		}
		Authentication authentication;
		try {
			authentication = authenticationManager
					.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, body.password()));
		}
		catch (AuthenticationException e) {
			rateLimiter.recordFailure(email, ip);
			// bad credentials, unknown user and inactive account all look the same
			throw new ErrorResponseException(HttpStatus.UNAUTHORIZED,
					ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password."), e);
		}
		HttpSession session = request.getSession(); // ensure a session exists so its id can be rotated
		onSignIn.onAuthentication(authentication, request, response);
		Instant now = clock.instant();
		session.setAttribute(SessionTimeoutFilter.AUTHENTICATED_AT, now);
		session.setAttribute(SessionTimeoutFilter.LAST_SEEN, now);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
		return accounts.findById(principal.id()).map(responses::of).orElseThrow();
	}

}
