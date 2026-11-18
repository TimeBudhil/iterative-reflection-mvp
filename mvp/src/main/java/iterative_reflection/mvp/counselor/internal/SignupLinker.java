package iterative_reflection.mvp.counselor.internal;

import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.StudentSignedUp;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Links a new student at signup (R2.2). A plain synchronous listener, so it runs inside the signup transaction:
 * an unknown code throws 404 and the account is never created.
 */
@Component
class SignupLinker {

	private final CounselorService counselors;

	SignupLinker(CounselorService counselors) {
		this.counselors = counselors;
	}

	@EventListener
	void on(StudentSignedUp event) {
		if (event.counselorCode() != null && !event.counselorCode().isBlank()) {
			counselors.link(event.userId(), event.counselorCode());
		}
	}

}
