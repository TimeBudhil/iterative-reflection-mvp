package iterative_reflection.mvp.identity.internal.repository;

import iterative_reflection.mvp.identity.internal.domain.UserEntity;
import iterative_reflection.mvp.identity.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByEmail(String email);

	Page<UserEntity> findByRoleOrderByIdDesc(Role role, Pageable pageable);

	@org.springframework.data.jpa.repository.Query("""
			select u.id from UserEntity u where u.role = iterative_reflection.mvp.identity.Role.STUDENT
			  and u.active = true and u.demo = false
			  and (cast(:from as timestamp) is null or u.createdAt >= :from)
			  and (cast(:to as timestamp) is null or u.createdAt < :to)""")
	java.util.Set<Long> pilotStudentIds(@org.springframework.data.repository.query.Param("from") java.time.Instant from,
			@org.springframework.data.repository.query.Param("to") java.time.Instant to);

	boolean existsByEmail(String email);

}
