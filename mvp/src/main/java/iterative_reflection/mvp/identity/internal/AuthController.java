package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	/** Session-fixation protection: new session id, and rotate the CSRF token, on every sign-in. */
	private final SessionAuthenticationStrategy onSignIn = new CompositeSessionAuthenticationStrategy(List.of(
			new ChangeSessionIdAuthenticationStrategy(),
			new CsrfAuthenticationStrategy(CookieCsrfTokenRepository.withHttpOnlyFalse())));

	AuthController(UserAccounts accounts, AuthenticationManager authenticationManager) {
		this.accounts = accounts;
		this.authenticationManager = authenticationManager;
	}

	@PostMapping("/api/auth/signup")
	@ResponseStatus(HttpStatus.CREATED)
	MeResponse signUp(@Valid @RequestBody SignupRequest request) {
		return MeResponse.of(accounts.signUpStudent(request));
	}

	@PostMapping("/api/auth/signin")
	MeResponse signIn(@Valid @RequestBody SignInRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken
					.unauthenticated(UserAccounts.normalizeEmail(body.email()), body.password()));
		}
		catch (AuthenticationException e) {
			// bad credentials, unknown user and inactive account all look the same
			throw new ErrorResponseException(HttpStatus.UNAUTHORIZED,
					ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password."), e);
		}
		request.getSession(); // ensure a session exists so its id can be rotated
		onSignIn.onAuthentication(authentication, request, response);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
		AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
		return accounts.findById(principal.id()).map(MeResponse::of).orElseThrow();
	}

}
