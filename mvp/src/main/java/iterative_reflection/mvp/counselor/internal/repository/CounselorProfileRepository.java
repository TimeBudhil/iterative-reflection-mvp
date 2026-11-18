package iterative_reflection.mvp.counselor.internal.repository;

import iterative_reflection.mvp.counselor.internal.domain.CounselorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounselorProfileRepository extends JpaRepository<CounselorProfileEntity, Long> {

	boolean existsByJoinCode(String joinCode);

	java.util.Optional<CounselorProfileEntity> findByJoinCode(String joinCode);

}
