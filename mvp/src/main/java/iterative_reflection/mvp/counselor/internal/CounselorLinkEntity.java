package iterative_reflection.mvp.counselor.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "counselor_links")
class CounselorLinkEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "student_id", nullable = false)
	long studentId;

	@Column(name = "counselor_id", nullable = false)
	long counselorId;

	@Column(name = "linked_at", nullable = false)
	Instant linkedAt;

	@Column(name = "revoked_at")
	Instant revokedAt;

}
