CREATE TABLE feed_state (
  student_id BIGINT PRIMARY KEY,
  career_id  BIGINT NOT NULL,
  served_at  TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT feed_state_student_fk FOREIGN KEY (student_id) REFERENCES users (id),
  CONSTRAINT feed_state_career_fk FOREIGN KEY (career_id) REFERENCES careers (id)
);
