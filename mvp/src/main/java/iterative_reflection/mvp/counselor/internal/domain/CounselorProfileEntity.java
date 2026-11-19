package iterative_reflection.mvp.counselor.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "counselor_profiles")
public class CounselorProfileEntity {

	@Id
	@Column(name = "user_id")
	public Long userId;

	@Column(name = "join_code", nullable = false)
	public String joinCode;

	@Column(name = "created_at", nullable = false)
	public Instant createdAt;

}
