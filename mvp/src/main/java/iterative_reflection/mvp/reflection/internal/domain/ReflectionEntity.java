package iterative_reflection.mvp.reflection.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** The (student, career) reflection header; versions and answers are written with JDBC. */
@Entity
@Table(name = "reflections")
public class ReflectionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Column(name = "student_id", nullable = false)
	long studentId;

	@Column(name = "career_id", nullable = false)
	long careerId;

	@Column(name = "current_version", nullable = false)
	public int currentVersion;

	@Version
	@Column(nullable = false)
	long version;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	public Instant updatedAt;

	protected ReflectionEntity() {
	}

	public ReflectionEntity(long studentId, long careerId, Instant now) {
		this.studentId = studentId;
		this.careerId = careerId;
		this.currentVersion = 1;
		this.createdAt = now;
		this.updatedAt = now;
	}

}
