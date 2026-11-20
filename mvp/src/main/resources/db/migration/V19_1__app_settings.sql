-- Generic app-wide settings (update 2). One row per setting key; value_json holds that setting's typed value.
-- No foreign key on updated_by: the settings module depends on no other module, and the value is only an audit hint.
CREATE TABLE app_settings (
  "key"      VARCHAR(100) PRIMARY KEY,
  value_json VARCHAR(2000) NOT NULL,
  updated_by BIGINT,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
