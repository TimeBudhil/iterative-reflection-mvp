package iterative_reflection.mvp.counselor.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "counselor_profiles")
class CounselorProfileEntity {

	@Id
	@Column(name = "user_id")
	Long userId;

	@Column(name = "join_code", nullable = false)
	String joinCode;

	@Column(name = "created_at", nullable = false)
	Instant createdAt;

}
