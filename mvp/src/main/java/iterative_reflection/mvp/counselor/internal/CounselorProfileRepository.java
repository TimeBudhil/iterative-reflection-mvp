package iterative_reflection.mvp.counselor.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface CounselorProfileRepository extends JpaRepository<CounselorProfileEntity, Long> {

	boolean existsByJoinCode(String joinCode);

	java.util.Optional<CounselorProfileEntity> findByJoinCode(String joinCode);

}
