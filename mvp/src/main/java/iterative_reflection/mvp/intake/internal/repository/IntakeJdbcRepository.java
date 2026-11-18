package iterative_reflection.mvp.intake.internal.repository;

import iterative_reflection.mvp.intake.internal.domain.IntakeRows.AnswerRow;
import iterative_reflection.mvp.intake.internal.domain.IntakeRows.AnswerTag;
import iterative_reflection.mvp.intake.internal.domain.IntakeRows.Question;
import iterative_reflection.mvp.intake.internal.domain.IntakeRows.ResponseHead;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Hand-written SQL over the intake tables. */
@Repository
public class IntakeJdbcRepository {

	private final JdbcClient jdbc;

	IntakeJdbcRepository(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public List<Question> activeQuestions() {
		return jdbc.sql("select id, question_key, prompt, question_type from intake_questions "
				+ "where active = true order by display_order, id")
				.query((rs, n) -> new Question(rs.getLong("id"), rs.getString("question_key"), rs.getString("prompt"),
						rs.getString("question_type")))
				.list();
	}

	/** @return the new response id; the unique student constraint turns a second insert into a DuplicateKeyException */
	public long insertResponse(long studentId, Instant now) throws DuplicateKeyException {
		GeneratedKeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("insert into intake_responses (student_id, created_at, updated_at) values (:s, :t, :t)")
				.param("s", studentId).param("t", Timestamp.from(now)).update(keys, "id");
		return keys.getKey().longValue();
	}

	public Optional<Long> responseIdOf(long studentId) {
		return jdbc.sql("select id from intake_responses where student_id = :s").param("s", studentId)
				.query(Long.class).optional();
	}

	/** Removes every answer (and its tags) of the response and stamps it as updated. */
	public void clearAnswers(long responseId, Instant now) {
		jdbc.sql("delete from intake_answer_tags where answer_id in (select id from intake_answers "
				+ "where response_id = :r)").param("r", responseId).update();
		jdbc.sql("delete from intake_answers where response_id = :r").param("r", responseId).update();
		jdbc.sql("update intake_responses set updated_at = :t where id = :r").param("t", Timestamp.from(now))
				.param("r", responseId).update();
	}

	/** @return the new answer id */
	public long insertAnswer(long responseId, long questionId, String text) {
		GeneratedKeyHolder keys = new GeneratedKeyHolder();
		jdbc.sql("insert into intake_answers (response_id, question_id, text_value) values (:r, :q, :v)")
				.param("r", responseId).param("q", questionId).param("v", text).update(keys, "id");
		return keys.getKey().longValue();
	}

	public void insertAnswerTag(long answerId, long tagId) {
		jdbc.sql("insert into intake_answer_tags (answer_id, tag_id) values (:a, :t)").param("a", answerId)
				.param("t", tagId).update();
	}

	public void insertDismissalIfAbsent(long studentId, Instant now) {
		jdbc.sql("insert into intake_dismissals (student_id, dismissed_at) select :s, :t "
				+ "where not exists (select 1 from intake_dismissals where student_id = :s)")
				.param("s", studentId).param("t", Timestamp.from(now)).update();
	}

	public boolean isDismissed(long studentId) {
		return jdbc.sql("select count(*) from intake_dismissals where student_id = :s").param("s", studentId)
				.query(Integer.class).single() > 0;
	}

	public boolean hasResponse(long studentId) {
		return jdbc.sql("select count(*) from intake_responses where student_id = :s").param("s", studentId)
				.query(Integer.class).single() > 0;
	}

	public Set<Long> studentsWithResponse(Collection<Long> studentIds) {
		if (studentIds.isEmpty()) {
			return Set.of();
		}
		return Set.copyOf(jdbc.sql("select student_id from intake_responses where student_id in (:ids)")
				.param("ids", studentIds).query(Long.class).list());
	}

	public Map<Long, Instant> lastUpdatedOf(Collection<Long> studentIds) {
		Map<Long, Instant> result = new HashMap<>();
		if (studentIds.isEmpty()) {
			return result;
		}
		jdbc.sql("select student_id, updated_at from intake_responses where student_id in (:ids)")
				.param("ids", studentIds).query((rs, n) -> {
					result.put(rs.getLong("student_id"), rs.getTimestamp("updated_at").toInstant());
					return 0;
				}).list();
		return result;
	}

	public Optional<ResponseHead> head(long studentId) {
		return jdbc.sql("select id, created_at, updated_at from intake_responses where student_id = :s")
				.param("s", studentId).query((rs, n) -> new ResponseHead(rs.getLong("id"),
						rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant()))
				.optional();
	}

	public List<AnswerTag> answerTags(long responseId) {
		return jdbc.sql("select at.answer_id, at.tag_id from intake_answer_tags at join intake_answers a "
				+ "on a.id = at.answer_id where a.response_id = :r order by at.tag_id").param("r", responseId)
				.query((rs, n) -> new AnswerTag(rs.getLong("answer_id"), rs.getLong("tag_id"))).list();
	}

	public List<AnswerRow> answers(long responseId) {
		return jdbc.sql("""
				select a.id, q.question_key, q.prompt, q.question_type, a.text_value
				from intake_answers a join intake_questions q on q.id = a.question_id
				where a.response_id = :r order by q.display_order, q.id""").param("r", responseId)
				.query((rs, n) -> new AnswerRow(rs.getLong("id"), rs.getString("question_key"),
						rs.getString("prompt"), rs.getString("question_type"), rs.getString("text_value")))
				.list();
	}

}
