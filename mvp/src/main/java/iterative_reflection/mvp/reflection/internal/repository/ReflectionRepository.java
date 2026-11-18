package iterative_reflection.mvp.reflection.internal.repository;

import iterative_reflection.mvp.reflection.internal.domain.ReflectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReflectionRepository extends JpaRepository<ReflectionEntity, Long> {

	java.util.Optional<ReflectionEntity> findByStudentIdAndCareerId(long studentId, long careerId);

	boolean existsByStudentIdAndCareerId(long studentId, long careerId);

}
