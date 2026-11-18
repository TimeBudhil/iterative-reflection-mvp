package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class MeController {

	private final UserAccounts accounts;

	private final MeResponses responses;

	MeController(UserAccounts accounts, MeResponses responses) {
		this.accounts = accounts;
		this.responses = responses;
	}

	@GetMapping("/api/me")
	MeResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return accounts.findById(principal.id()).map(responses::of)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
	}

}
