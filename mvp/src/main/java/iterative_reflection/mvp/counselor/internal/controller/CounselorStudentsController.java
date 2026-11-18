package iterative_reflection.mvp.counselor.internal.controller;

import iterative_reflection.mvp.counselor.internal.services.StudentDashboard;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.counselor.internal.services.StudentAccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Read-only counselor dashboard. Every route here must be GET (R8.4). */
@RestController
public class CounselorStudentsController {

	private final StudentDashboard dashboard;

	CounselorStudentsController(StudentDashboard dashboard) {
		this.dashboard = dashboard;
	}

	@GetMapping("/api/counselor/students/{studentId}")
	StudentDashboard.Detail detail(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long studentId) {
		return dashboard.detail(me.id(), studentId);
	}

	@ExceptionHandler(StudentAccessDeniedException.class)
	ProblemDetail accessDenied(StudentAccessDeniedException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
	}

	@GetMapping("/api/counselor/students")
	Page<StudentDashboard.Row> students(@AuthenticationPrincipal AppUserPrincipal me, Pageable pageable) {
		return dashboard.students(me.id(), pageable);
	}

}
