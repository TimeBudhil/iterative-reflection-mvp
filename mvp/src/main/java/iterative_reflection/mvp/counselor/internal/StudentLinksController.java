package iterative_reflection.mvp.counselor.internal;

import iterative_reflection.mvp.counselor.CounselorLink;
import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A student's own counselor links: link by join code, list, revoke (R2.2-R2.6). */
@RestController
class StudentLinksController {

	record LinkRequest(@NotBlank @Size(max = 32) String code) {
	}

	private final CounselorService counselors;

	private final LinkAttemptLimiter limiter;

	StudentLinksController(CounselorService counselors, LinkAttemptLimiter limiter) {
		this.counselors = counselors;
		this.limiter = limiter;
	}

	@PostMapping("/api/students/me/counselors")
	@ResponseStatus(HttpStatus.CREATED)
	CounselorLink link(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody LinkRequest request) {
		if (limiter.isBlocked(me.id())) {
			throw new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS, ProblemDetail.forStatusAndDetail(
					HttpStatus.TOO_MANY_REQUESTS, "Too many failed link attempts. Try again later."), null);
		}
		try {
			return counselors.link(me.id(), request.code());
		}
		catch (ErrorResponseException e) {
			if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
				limiter.recordFailure(me.id());
			}
			throw e;
		}
	}

	@GetMapping("/api/students/me/counselors")
	Page<CounselorLink> list(@AuthenticationPrincipal AppUserPrincipal me, Pageable pageable) {
		return counselors.linksOf(me.id(), pageable);
	}

	@DeleteMapping("/api/students/me/counselors/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revoke(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
		counselors.revoke(me.id(), id);
	}

}
