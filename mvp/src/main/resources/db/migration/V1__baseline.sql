-- Baseline migration: platform infrastructure only. Each module adds its own tables in later versioned migrations.
-- Every migration must run on both H2 (PostgreSQL mode) and PostgreSQL; use TIMESTAMP WITH TIME ZONE for timestamps.

-- Spring Modulith event publication registry (spring-modulith-starter-jpa)
CREATE TABLE event_publication (
    id                     UUID                     NOT NULL PRIMARY KEY,
    listener_id            VARCHAR                  NOT NULL,
    event_type             VARCHAR                  NOT NULL,
    serialized_event       VARCHAR                  NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 VARCHAR(32),
    completion_attempts    INTEGER                  NOT NULL DEFAULT 0,
    last_resubmission_date TIMESTAMP WITH TIME ZONE
);

CREATE INDEX event_publication_completion_date_idx ON event_publication (completion_date);
