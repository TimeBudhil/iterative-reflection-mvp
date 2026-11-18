package iterative_reflection.mvp.admin.internal;

import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminCounselorController {

	record CreateCounselorRequest(
			@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Size(max = 100) String displayName,
			@NotNull @Size(min = 12, max = 72, message = "must be between 12 and 72 characters") String temporaryPassword) {
	}

	record CounselorResponse(long id, String email, String displayName, String joinCode, boolean active,
			boolean mustChangePassword) {
	}

	private final UserService users;

	private final CounselorService counselors;

	AdminCounselorController(UserService users, CounselorService counselors) {
		this.users = users;
		this.counselors = counselors;
	}

	/** One transaction: the user and its profile are created together or not at all. */
	@PostMapping("/api/admin/counselors")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	CounselorResponse create(@Valid @RequestBody CreateCounselorRequest request) {
		UserSummary user = users.createCounselor(request.email(), request.displayName(),
				request.temporaryPassword());
		String code = counselors.createProfile(user.id()).joinCode();
		return new CounselorResponse(user.id(), user.email(), user.displayName(), code, user.active(),
				user.mustChangePassword());
	}

}
