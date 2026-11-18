package iterative_reflection.mvp.reflection.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReflectionRepository extends JpaRepository<ReflectionEntity, Long> {

	java.util.Optional<ReflectionEntity> findByStudentIdAndCareerId(long studentId, long careerId);

	boolean existsByStudentIdAndCareerId(long studentId, long careerId);

}
