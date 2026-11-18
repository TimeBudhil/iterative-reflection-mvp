package iterative_reflection.mvp.identity.internal;

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
class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(nullable = false)
	String email;

	@Column(name = "password_hash", nullable = false)
	String passwordHash;

	@Column(name = "display_name", nullable = false)
	String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	Role role;

	boolean active;

	@Column(name = "must_change_password")
	boolean mustChangePassword;

	boolean demo;

	@Column(name = "age_verified_at")
	Instant ageVerifiedAt;

	@Column(name = "accepted_terms_version")
	String acceptedTermsVersion;

	@Column(name = "accepted_terms_at")
	Instant acceptedTermsAt;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

	@Version
	long version;

}
