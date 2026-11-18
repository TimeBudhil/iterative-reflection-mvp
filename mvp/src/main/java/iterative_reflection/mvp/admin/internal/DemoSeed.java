package iterative_reflection.mvp.admin.internal;

import iterative_reflection.mvp.counselor.CounselorService;
import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.identity.UserService;
import iterative_reflection.mvp.identity.UserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dev demo data (R10.1, R10.3): 1 admin, 1 counselor and 2 students linked to that counselor, all flagged
 * {@code demo} with password {@code demo}. Only active when {@code app.demo-seed.enabled=true}, which only
 * {@code application-dev.properties} sets (the test base class turns it off). Re-running never duplicates:
 * each account is created only if its email is absent, and links are made only for newly created students.
 */
@Component
@ConditionalOnProperty("app.demo-seed.enabled")
class DemoSeed implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoSeed.class);

	static final String PASSWORD = "demo";

	private final UserService users;

	private final CounselorService counselors;

	DemoSeed(UserService users, CounselorService counselors) {
		this.users = users;
		this.counselors = counselors;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (users.findByEmail("admin@demo.example").isEmpty()) {
			users.createDemoAccount("admin@demo.example", "Demo Admin", PASSWORD, Role.ADMIN);
		}
		UserSummary counselor = users.findByEmail("counselor@demo.example").orElse(null);
		if (counselor == null) {
			counselor = users.createDemoAccount("counselor@demo.example", "Demo Counselor", PASSWORD,
					Role.COUNSELOR);
			counselors.createProfile(counselor.id());
		}
		String code = counselors.findByUserId(counselor.id()).orElseThrow().joinCode();
		for (int i = 1; i <= 2; i++) {
			String email = "student" + i + "@demo.example";
			if (users.findByEmail(email).isEmpty()) {
				UserSummary student = users.createDemoAccount(email, "Demo Student " + i, PASSWORD, Role.STUDENT);
				counselors.link(student.id(), code);
			}
		}
		log.info("Demo accounts ready (password \"{}\"): admin@, counselor@, student1@, student2@demo.example", PASSWORD);
	}

}
