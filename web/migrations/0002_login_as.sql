-- The software owner can open a school's headmaster account. Set only on those sessions.
ALTER TABLE sessions ADD COLUMN acting_owner_id INTEGER REFERENCES users(id);

-- Children's data is visible in that view, so every opening is recorded.
CREATE TABLE owner_access_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  owner_id INTEGER NOT NULL,
  school_id INTEGER NOT NULL,
  headmaster_id INTEGER NOT NULL,
  started_at INTEGER NOT NULL
);
