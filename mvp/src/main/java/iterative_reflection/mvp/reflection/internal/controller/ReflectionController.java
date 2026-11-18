package iterative_reflection.mvp.reflection.internal.controller;

import iterative_reflection.mvp.reflection.internal.services.ReflectionService;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.QuestionView;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.ReflectionView;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.SubmitRequest;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.VersionView;
import iterative_reflection.mvp.reflection.internal.dto.ReflectionDtos.SubmitResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReflectionController {

	private final ReflectionService service;

	ReflectionController(ReflectionService service) {
		this.service = service;
	}

	@GetMapping("/api/reflection-questions")
	List<QuestionView> questions() {
		return service.activeQuestions();
	}

	@PostMapping("/api/careers/{careerId}/reflection")
	@ResponseStatus(HttpStatus.CREATED)
	SubmitResponse submit(@PathVariable long careerId, @Valid @RequestBody SubmitRequest request,
			@AuthenticationPrincipal AppUserPrincipal user) {
		requireStudent(user);
		return service.submit(user.id(), careerId, request);
	}

	@PutMapping("/api/careers/{careerId}/reflection")
	SubmitResponse edit(@PathVariable long careerId, @Valid @RequestBody SubmitRequest request,
			@AuthenticationPrincipal AppUserPrincipal user) {
		requireStudent(user);
		return service.edit(user.id(), careerId, request);
	}

	@GetMapping("/api/careers/{careerId}/reflection/versions")
	Page<VersionView> versions(@PathVariable long careerId, Pageable pageable,
			@AuthenticationPrincipal AppUserPrincipal user) {
		requireStudent(user);
		return service.versions(user.id(), careerId, pageable);
	}

	@GetMapping("/api/students/me/reflections")
	Page<ReflectionView> mine(Pageable pageable, @AuthenticationPrincipal AppUserPrincipal user) {
		requireStudent(user);
		return service.list(user.id(), pageable);
	}

	private static void requireStudent(AppUserPrincipal user) {
		if (user.role() != Role.STUDENT) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
	}

}
