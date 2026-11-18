package iterative_reflection.mvp.counselor.internal.controller;

import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CounselorMeController {

	record CounselorMeResponse(long id, String email, String displayName, String joinCode) {
	}

	private final CounselorService counselors;

	CounselorMeController(CounselorService counselors) {
		this.counselors = counselors;
	}

	record RotateResponse(String joinCode) {
	}

	/** Allowed state-changing counselor route (R8.4 allowlist). */
	@PostMapping("/api/counselor/me/join-code/rotate")
	RotateResponse rotate(@AuthenticationPrincipal AppUserPrincipal me) {
		return new RotateResponse(counselors.rotateJoinCode(me.id()).joinCode());
	}

	@GetMapping("/api/counselor/me")
	CounselorMeResponse me(@AuthenticationPrincipal AppUserPrincipal me) {
		return counselors.findByUserId(me.id())
				.map(p -> new CounselorMeResponse(me.id(), me.email(), me.displayName(), p.joinCode()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
