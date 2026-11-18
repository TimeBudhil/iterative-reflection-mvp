package iterative_reflection.mvp.intake.internal;

import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.intake.IntakeView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
class IntakeController {

	private final IntakeService intake;

	IntakeController(IntakeService intake) {
		this.intake = intake;
	}

	@GetMapping("/api/intake/questions")
	List<IntakeDtos.QuestionView> questions() {
		return intake.questions();
	}

	@GetMapping("/api/intake")
	IntakeView mine(@AuthenticationPrincipal AppUserPrincipal me) {
		return intake.findIntake(me.id()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@PostMapping("/api/intake")
	@ResponseStatus(HttpStatus.CREATED)
	IntakeView submit(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody IntakeDtos.IntakeRequest body) {
		return intake.submit(me.id(), body);
	}

	@PutMapping("/api/intake")
	IntakeView edit(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody IntakeDtos.IntakeRequest body) {
		return intake.edit(me.id(), body);
	}

	@PostMapping("/api/intake/dismiss")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void dismiss(@AuthenticationPrincipal AppUserPrincipal me) {
		intake.dismiss(me.id());
	}

}
