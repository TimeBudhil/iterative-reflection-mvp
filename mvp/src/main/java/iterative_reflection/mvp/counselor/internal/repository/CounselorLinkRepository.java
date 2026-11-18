package iterative_reflection.mvp.counselor.internal.repository;

import iterative_reflection.mvp.counselor.internal.domain.CounselorLinkEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounselorLinkRepository extends JpaRepository<CounselorLinkEntity, Long> {

	Optional<CounselorLinkEntity> findByStudentIdAndCounselorId(long studentId, long counselorId);

	Optional<CounselorLinkEntity> findByIdAndStudentIdAndRevokedAtIsNull(long id, long studentId);

	Page<CounselorLinkEntity> findByStudentIdAndRevokedAtIsNullOrderByIdAsc(long studentId, Pageable pageable);

	boolean existsByCounselorIdAndStudentIdAndRevokedAtIsNull(long counselorId, long studentId);

	@org.springframework.data.jpa.repository.Query("select l.studentId from CounselorLinkEntity l "
			+ "where l.counselorId = :counselorId and l.revokedAt is null order by l.studentId")
	java.util.List<Long> findActiveStudentIds(
			@org.springframework.data.repository.query.Param("counselorId") long counselorId);

	long countByCounselorIdAndRevokedAtIsNull(long counselorId);

}
