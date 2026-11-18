package iterative_reflection.mvp.counselor.internal;

import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class CounselorMeController {

	record CounselorMeResponse(long id, String email, String displayName, String joinCode) {
	}

	private final CounselorService counselors;

	CounselorMeController(CounselorService counselors) {
		this.counselors = counselors;
	}

	@GetMapping("/api/counselor/me")
	CounselorMeResponse me(@AuthenticationPrincipal AppUserPrincipal me) {
		return counselors.findByUserId(me.id())
				.map(p -> new CounselorMeResponse(me.id(), me.email(), me.displayName(), p.joinCode()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
