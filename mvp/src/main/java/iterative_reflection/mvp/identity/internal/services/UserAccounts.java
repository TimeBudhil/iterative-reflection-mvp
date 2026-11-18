package iterative_reflection.mvp.identity.internal.services;

import iterative_reflection.mvp.identity.internal.domain.UserEntity;
import iterative_reflection.mvp.identity.internal.dto.SignupRequest;
import iterative_reflection.mvp.identity.internal.repository.UserRepository;
import iterative_reflection.mvp.identity.AppUserPrincipal;
import iterative_reflection.mvp.identity.Role;
import iterative_reflection.mvp.identity.StudentSignedUp;
import iterative_reflection.mvp.identity.UserService;
import org.springframework.context.ApplicationEventPublisher;
import iterative_reflection.mvp.identity.UserSummary;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

/** User persistence and signup rules; also the {@link UserService} public API and sign-in user lookup. */
@Service
public class UserAccounts implements UserService, UserDetailsService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final Clock clock;

	private final int minimumAge;

	private final ApplicationEventPublisher events;

	private final FindByIndexNameSessionRepository<? extends Session> sessions;

	UserAccounts(UserRepository users, PasswordEncoder passwordEncoder, Clock clock,
			@Value("${identity.minimum-age:13}") int minimumAge, ApplicationEventPublisher events,
			FindByIndexNameSessionRepository<? extends Session> sessions) {
		this.sessions = sessions;
		this.events = events;
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.minimumAge = minimumAge;
	}

	public static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	@Transactional
	public UserSummary signUpStudent(SignupRequest request) {
		LocalDate today = LocalDate.now(clock);
		if (Period.between(request.birthdate(), today).getYears() < minimumAge) {
			throw problem(HttpStatus.BAD_REQUEST, "You must be at least " + minimumAge + " years old to sign up.");
		}
		if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw problem(HttpStatus.BAD_REQUEST, "Password is too long.");
		}
		String email = normalizeEmail(request.email());
		if (users.existsByEmail(email)) {
			throw emailTaken();
		}
		Instant now = clock.instant();
		UserEntity user = new UserEntity();
		user.email = email;
		user.passwordHash = passwordEncoder.encode(request.password());
		user.displayName = request.displayName().trim();
		user.role = Role.STUDENT;
		user.active = true;
		user.ageVerifiedAt = now;
		user.acceptedTermsVersion = request.acceptedTermsVersion().trim();
		user.acceptedTermsAt = now;
		user.createdAt = now;
		UserSummary created;
		try {
			created = toSummary(users.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException e) {
			throw emailTaken();
		}
		events.publishEvent(new StudentSignedUp(created.id(), request.counselorCode()));
		return created;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserSummary> findByEmail(String email) {
		return users.findByEmail(normalizeEmail(email)).map(UserAccounts::toSummary);
	}

	@Override
	@Transactional
	public UserSummary createDemoAccount(String email, String displayName, String password, Role role) {
		UserEntity user = new UserEntity();
		user.email = normalizeEmail(email);
		user.passwordHash = passwordEncoder.encode(password);
		user.displayName = displayName.trim();
		user.role = role;
		user.active = true;
		user.demo = true;
		user.createdAt = clock.instant();
		return toSummary(users.saveAndFlush(user));
	}

	@Override
	@Transactional
	public UserSummary createCounselor(String email, String displayName, String temporaryPassword) {
		return createAccount(email, displayName, temporaryPassword, Role.COUNSELOR, true);
	}

	/** Creates a staff user (no terms/age record). Used for counselors and the bootstrap admin. */
	@Transactional
	UserSummary createAccount(String email, String displayName, String password, Role role,
			boolean mustChangePassword) {
		if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw problem(HttpStatus.BAD_REQUEST, "Password must be between 12 and 72 characters.");
		}
		String normalized = normalizeEmail(email);
		if (users.existsByEmail(normalized)) {
			throw emailTaken();
		}
		UserEntity user = new UserEntity();
		user.email = normalized;
		user.passwordHash = passwordEncoder.encode(password);
		user.displayName = displayName.trim();
		user.role = role;
		user.active = true;
		user.mustChangePassword = mustChangePassword;
		user.createdAt = clock.instant();
		try {
			return toSummary(users.saveAndFlush(user));
		}
		catch (DataIntegrityViolationException e) {
			throw emailTaken();
		}
	}

	/** Verifies the current password, stores the new one and clears {@code mustChangePassword}. */
	@Transactional
	public void changePassword(long id, String currentPassword, String newPassword) {
		UserEntity user = users.findById(id).orElseThrow();
		if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
			throw problem(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
		}
		if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw problem(HttpStatus.BAD_REQUEST, "Password is too long.");
		}
		user.passwordHash = passwordEncoder.encode(newPassword);
		user.mustChangePassword = false;
		users.saveAndFlush(user);
	}

	/** Deletes every stored session of the user except {@code keepSessionId} (the caller's own). */
	public void endOtherSessions(String email, String keepSessionId) {
		sessions.findByPrincipalName(email).keySet().stream().filter(id -> !id.equals(keepSessionId))
				.forEach(sessions::deleteById);
	}

	@Transactional
	public UserSummary updateDisplayName(long id, String displayName) {
		UserEntity user = users.findById(id).orElseThrow();
		user.displayName = displayName.trim();
		return toSummary(users.saveAndFlush(user));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<UserSummary> findByRole(Role role, Pageable pageable) {
		return users.findByRoleOrderByIdDesc(role, pageable).map(UserAccounts::toSummary);
	}

	@Override
	@Transactional(readOnly = true)
	public long countPilotStudents(Instant from, Instant to) {
		return users.countPilotStudents(from, to);
	}

	@Override
	@Transactional
	public UserSummary setActive(long userId, boolean active) {
		UserEntity user = users.findById(userId)
				.orElseThrow(() -> problem(HttpStatus.NOT_FOUND, "No such user."));
		user.active = active;
		UserSummary saved = toSummary(users.saveAndFlush(user));
		if (!active) {
			// Session principal name is the lower-cased email (see loadUserByUsername).
			sessions.findByPrincipalName(user.email).keySet().forEach(sessions::deleteById);
		}
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserSummary> findById(long id) {
		return users.findById(id).map(UserAccounts::toSummary);
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserEntity u = users.findByEmail(normalizeEmail(username))
				.orElseThrow(() -> new UsernameNotFoundException("unknown user"));
		return new AppUserPrincipal(u.id, u.email, u.displayName, u.role, u.active, u.mustChangePassword,
				u.passwordHash);
	}

	private static UserSummary toSummary(UserEntity u) {
		return new UserSummary(u.id, u.email, u.displayName, u.role, u.active, u.mustChangePassword);
	}

	private static ErrorResponseException emailTaken() {
		return problem(HttpStatus.CONFLICT, "An account with this email already exists.");
	}

	private static ErrorResponseException problem(HttpStatus status, String detail) {
		return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), null);
	}

}
