package iterative_reflection.mvp.reflection.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReflectionRepository extends JpaRepository<ReflectionEntity, Long> {

	boolean existsByStudentIdAndCareerId(long studentId, long careerId);

}
