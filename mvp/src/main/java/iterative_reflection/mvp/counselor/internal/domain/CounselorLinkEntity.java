package iterative_reflection.mvp.counselor.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "counselor_links")
public class CounselorLinkEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long id;

	@Column(name = "student_id", nullable = false)
	public long studentId;

	@Column(name = "counselor_id", nullable = false)
	public long counselorId;

	@Column(name = "linked_at", nullable = false)
	public Instant linkedAt;

	@Column(name = "revoked_at")
	public Instant revokedAt;

}
