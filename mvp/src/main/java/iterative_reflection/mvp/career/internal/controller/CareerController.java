package iterative_reflection.mvp.career.internal.controller;

import iterative_reflection.mvp.career.internal.dto.ExperienceResponse;
import iterative_reflection.mvp.career.internal.services.CareerService;
import iterative_reflection.mvp.career.CareerSummary;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.identity.Role;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only career endpoints; the only write is the student's own page progress. */
@RestController
public class CareerController {

	private final CareerService catalog;

	CareerController(CareerService catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/api/careers/{id}/experience")
	ExperienceResponse experience(@PathVariable long id, @AuthenticationPrincipal AppUserPrincipal user) {
		CareerSummary career = activeCareer(id);
		return ExperienceResponse.of(career, catalog.lastViewedPage(user.id(), id), catalog.pages(id));
	}

	@PostMapping("/api/careers/{id}/pages/{n}/viewed")
	Map<String, Integer> viewed(@PathVariable long id, @PathVariable int n,
			@AuthenticationPrincipal AppUserPrincipal user) {
		if (user.role() != Role.STUDENT) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		CareerSummary career = activeCareer(id);
		if (n < 1 || n > career.lastPage()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return Map.of("lastViewedPage", catalog.recordViewed(user.id(), id, n));
	}

	private CareerSummary activeCareer(long id) {
		return catalog.findCareer(id).filter(CareerSummary::active)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

}
