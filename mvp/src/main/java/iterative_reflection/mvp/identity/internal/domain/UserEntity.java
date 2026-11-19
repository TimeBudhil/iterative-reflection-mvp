package iterative_reflection.mvp.identity.internal.domain;

import iterative_reflection.mvp.identity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Column(nullable = false)
	public String email;

	@Column(name = "password_hash", nullable = false)
	public String passwordHash;

	@Column(name = "display_name", nullable = false)
	public String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	public Role role;

	public boolean active;

	@Column(name = "must_change_password")
	public boolean mustChangePassword;

	public boolean demo;

	@Column(name = "age_verified_at")
	public Instant ageVerifiedAt;

	@Column(name = "accepted_terms_version")
	public String acceptedTermsVersion;

	@Column(name = "accepted_terms_at")
	public Instant acceptedTermsAt;

	@Column(name = "created_at", nullable = false)
	public Instant createdAt;

	@Version
	long version;

}
