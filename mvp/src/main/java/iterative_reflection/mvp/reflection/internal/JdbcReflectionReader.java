package iterative_reflection.mvp.reflection.internal;

import iterative_reflection.mvp.reflection.ReflectionReader;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
class JdbcReflectionReader implements ReflectionReader {

	private final JdbcClient jdbc;

	JdbcReflectionReader(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public int reflectionCount(long studentId) {
		return jdbc.sql("select count(*) from reflections where student_id = :s").param("s", studentId)
				.query(Integer.class).single();
	}

	@Override
	public Set<Long> reflectedCareerIds(long studentId) {
		return new HashSet<>(jdbc.sql("select career_id from reflections where student_id = :s")
				.param("s", studentId).query(Long.class).list());
	}

}
