package iterative_reflection.mvp.identity.internal;

import iterative_reflection.mvp.identity.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByEmail(String email);

	Page<UserEntity> findByRoleOrderByIdDesc(Role role, Pageable pageable);

	boolean existsByEmail(String email);

}
