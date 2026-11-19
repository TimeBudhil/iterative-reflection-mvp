CREATE TABLE counselor_profiles (
  user_id    BIGINT PRIMARY KEY,
  join_code  VARCHAR(8) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT counselor_profiles_user_fk FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT counselor_profiles_code_uq UNIQUE (join_code)
);
