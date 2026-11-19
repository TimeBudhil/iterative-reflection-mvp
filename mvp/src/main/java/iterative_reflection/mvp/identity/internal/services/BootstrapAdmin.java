package iterative_reflection.mvp.identity.internal.services;

import iterative_reflection.mvp.identity.internal.repository.UserRepository;
import iterative_reflection.mvp.identity.Role;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Creates the first admin from {@code ADMIN_EMAIL}/{@code ADMIN_PASSWORD} (R9.1). Idempotent: an existing user
 * with that email is left alone. The password is trusted, so no forced change. A password that breaks the policy
 * fails startup.
 */
@Component
public class BootstrapAdmin implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

	private final UserAccounts accounts;

	private final UserRepository users;

	private final String email;

	private final String password;

	BootstrapAdmin(UserAccounts accounts, UserRepository users, @Value("${app.admin.email:}") String email,
			@Value("${app.admin.password:}") String password) {
		this.accounts = accounts;
		this.users = users;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank()) {
			return;
		}
		if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("ADMIN_PASSWORD must be between 12 and 72 characters");
		}
		if (users.existsByEmail(UserAccounts.normalizeEmail(email))) {
			return;
		}
		accounts.createAccount(email, "Administrator", password, Role.ADMIN, false);
		log.info("Bootstrap admin created");
	}

}
