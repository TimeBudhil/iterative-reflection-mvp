package iterative_reflection.mvp.admin.internal.controller;

import iterative_reflection.mvp.admin.internal.services.AdminActivity;
import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import iterative_reflection.mvp.counselor.CounselorOverview;
import iterative_reflection.mvp.identity.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminCounselorController {

	record CreateCounselorRequest(
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(max = 100) String displayName,
			@NotNull @Size(min = 12, max = 72, message = "must be between 12 and 72 characters") String temporaryPassword) {
	}

	record CounselorResponse(long id, String email, String displayName, String joinCode, boolean active,
			boolean mustChangePassword) {
	}

	record ListItem(long id, String email, String displayName, String joinCode, boolean active,
			long linkedStudentCount) {
	}

	record SetActiveRequest(@NotNull Boolean active) {
	}

	private final UserService users;

	private final CounselorService counselors;

	private final AdminActivity activity;

	AdminCounselorController(UserService users, CounselorService counselors, AdminActivity activity) {
		this.users = users;
		this.counselors = counselors;
		this.activity = activity;
	}

	/** One transaction: the user and its profile are created together or not at all. */
	@PostMapping("/api/admin/counselors")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	CounselorResponse create(@Valid @RequestBody CreateCounselorRequest request,
			@AuthenticationPrincipal AppUserPrincipal me) {
		UserSummary user = users.createCounselor(request.email(), request.displayName(),
				request.temporaryPassword());
		String code = counselors.createProfile(user.id()).joinCode();
		activity.recordAfterCommit(me.id(), AdminActivity.COUNSELOR_CREATED, Map.of("email", user.email()));
		return new CounselorResponse(user.id(), user.email(), user.displayName(), code, user.active(),
				user.mustChangePassword());
	}

	@GetMapping("/api/admin/counselors")
	@Transactional(readOnly = true)
	Page<ListItem> list(Pageable pageable) {
		Page<UserSummary> page = users.findByRole(Role.COUNSELOR, pageable);
		Map<Long, CounselorOverview> overviews = counselors
				.overviews(page.getContent().stream().map(UserSummary::id).toList());
		return page.map(u -> {
			CounselorOverview o = overviews.get(u.id());
			return new ListItem(u.id(), u.email(), u.displayName(), o == null ? null : o.joinCode(), u.active(),
					o == null ? 0 : o.linkedStudentCount());
		});
	}

	@PatchMapping("/api/admin/counselors/{id}")
	@Transactional
	CounselorResponse setActive(@PathVariable long id, @Valid @RequestBody SetActiveRequest request,
			@AuthenticationPrincipal AppUserPrincipal me) {
		UserSummary existing = users.findById(id).filter(u -> u.role() == Role.COUNSELOR)
				.orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND,
						ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No such counselor."), null));
		UserSummary user = users.setActive(existing.id(), request.active());
		if (existing.active() != request.active()) {
			activity.recordAfterCommit(me.id(),
					request.active() ? AdminActivity.COUNSELOR_REACTIVATED : AdminActivity.COUNSELOR_DEACTIVATED,
					Map.of("email", user.email()));
		}
		String code = counselors.findByUserId(id).map(p -> p.joinCode()).orElse(null);
		return new CounselorResponse(user.id(), user.email(), user.displayName(), code, user.active(),
				user.mustChangePassword());
	}

}
