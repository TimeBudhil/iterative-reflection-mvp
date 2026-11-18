package iterative_reflection.mvp.counselor.internal;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface CounselorLinkRepository extends JpaRepository<CounselorLinkEntity, Long> {

	Optional<CounselorLinkEntity> findByStudentIdAndCounselorId(long studentId, long counselorId);

	Optional<CounselorLinkEntity> findByIdAndStudentIdAndRevokedAtIsNull(long id, long studentId);

	Page<CounselorLinkEntity> findByStudentIdAndRevokedAtIsNullOrderByIdAsc(long studentId, Pageable pageable);

	boolean existsByCounselorIdAndStudentIdAndRevokedAtIsNull(long counselorId, long studentId);

	long countByCounselorIdAndRevokedAtIsNull(long counselorId);

}
