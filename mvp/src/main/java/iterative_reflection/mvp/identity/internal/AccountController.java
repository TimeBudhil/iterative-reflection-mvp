package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Self-service account changes: password and display name. */
@RestController
class AccountController {

	private final UserAccounts accounts;

	private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

	private final SessionAuthenticationStrategy rotateSession = new CompositeSessionAuthenticationStrategy(List.of(
			new ChangeSessionIdAuthenticationStrategy(),
			new CsrfAuthenticationStrategy(CookieCsrfTokenRepository.withHttpOnlyFalse())));

	AccountController(UserAccounts accounts) {
		this.accounts = accounts;
	}

	/** Rotates the session id and refreshes the stored principal (so {@code mustChangePassword} is cleared). */
	@PutMapping("/api/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changePassword(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		accounts.changePassword(principal.id(), body.currentPassword(), body.newPassword());
		refreshSession(principal.email(), true, request, response);
	}

	@PatchMapping("/api/me")
	MeResponse updateProfile(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody UpdateProfileRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		MeResponse me = MeResponse.of(accounts.updateDisplayName(principal.id(), body.displayName()));
		refreshSession(principal.email(), false, request, response);
		return me;
	}

	private void refreshSession(String email, boolean rotate, HttpServletRequest request,
			HttpServletResponse response) {
		AppUserPrincipal fresh = (AppUserPrincipal) accounts.loadUserByUsername(email);
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(fresh, null,
				fresh.getAuthorities());
		if (rotate) {
			rotateSession.onAuthentication(authentication, request, response);
		}
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}

}
