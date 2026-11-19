package iterative_reflection.mvp.identity;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The signed-in user as kept in the session. Obtain it in a controller with
 * {@code @AuthenticationPrincipal AppUserPrincipal me}. The username (and session principal name) is the
 * lower-cased email, which never changes.
 */
public final class AppUserPrincipal implements UserDetails, CredentialsContainer {

	private static final long serialVersionUID = 1L;

	/** The only authority held while {@link #mustChangePassword()} is true. */
	public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

	private final long id;

	private final String email;

	private final String displayName;

	private final Role role;

	private final boolean active;

	private final boolean mustChangePassword;

	private String passwordHash;

	public AppUserPrincipal(long id, String email, String displayName, Role role, boolean active,
			boolean mustChangePassword, String passwordHash) {
		this.id = id;
		this.email = email;
		this.displayName = displayName;
		this.role = role;
		this.active = active;
		this.mustChangePassword = mustChangePassword;
		this.passwordHash = passwordHash;
	}

	public long id() {
		return id;
	}

	public String email() {
		return email;
	}

	public String displayName() {
		return displayName;
	}

	public Role role() {
		return role;
	}

	public boolean mustChangePassword() {
		return mustChangePassword;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// A user who must change a temporary password holds only this authority, so every role-based rule denies
		// them (R1.10); SecurityConfig lists the few endpoints that stay open. The role itself stays in role().
		if (mustChangePassword) {
			return List.of(new SimpleGrantedAuthority(PASSWORD_CHANGE_REQUIRED));
		}
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

}
