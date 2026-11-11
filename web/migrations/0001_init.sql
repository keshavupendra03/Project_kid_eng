-- Schools, staff, classes, students and the learning summaries phones send.
-- Voice recordings and other-app usage are deliberately not stored here.

CREATE TABLE schools (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  created_at INTEGER NOT NULL
);

-- role: owner (no school), headmaster, teacher
CREATE TABLE users (
  id INTEGER PRIMARY KEY,
  school_id INTEGER REFERENCES schools(id),
  role TEXT NOT NULL CHECK (role IN ('owner', 'headmaster', 'teacher')),
  name TEXT NOT NULL,
  username TEXT NOT NULL UNIQUE COLLATE NOCASE,
  pass_hash TEXT NOT NULL,
  pass_salt TEXT NOT NULL,
  must_change_password INTEGER NOT NULL DEFAULT 1,
  failed_logins INTEGER NOT NULL DEFAULT 0,
  locked_until INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX idx_users_school ON users(school_id, role);

CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id),
  expires_at INTEGER NOT NULL
);
CREATE INDEX idx_sessions_user ON sessions(user_id);

CREATE TABLE classes (
  id INTEGER PRIMARY KEY,
  school_id INTEGER NOT NULL REFERENCES schools(id),
  grade INTEGER NOT NULL,
  section TEXT NOT NULL,
  UNIQUE (school_id, grade, section)
);

CREATE TABLE teacher_classes (
  teacher_id INTEGER NOT NULL REFERENCES users(id),
  class_id INTEGER NOT NULL REFERENCES classes(id),
  PRIMARY KEY (teacher_id, class_id)
);
CREATE INDEX idx_teacher_classes_class ON teacher_classes(class_id);

-- One parent per phone number per school, so siblings share a parent record.
CREATE TABLE parents (
  id INTEGER PRIMARY KEY,
  school_id INTEGER NOT NULL REFERENCES schools(id),
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  UNIQUE (school_id, phone)
);

CREATE TABLE students (
  id INTEGER PRIMARY KEY,
  school_id INTEGER NOT NULL REFERENCES schools(id),
  class_id INTEGER NOT NULL REFERENCES classes(id),
  name TEXT NOT NULL,
  join_code TEXT UNIQUE,
  device_token_hash TEXT UNIQUE,
  consent_at INTEGER,
  consent_version TEXT,
  created_at INTEGER NOT NULL
);
CREATE INDEX idx_students_class ON students(class_id);
CREATE INDEX idx_students_school ON students(school_id);

CREATE TABLE student_parents (
  student_id INTEGER NOT NULL REFERENCES students(id),
  parent_id INTEGER NOT NULL REFERENCES parents(id),
  PRIMARY KEY (student_id, parent_id)
);
CREATE INDEX idx_student_parents_parent ON student_parents(parent_id);

CREATE TABLE student_summary (
  student_id INTEGER PRIMARY KEY REFERENCES students(id),
  lessons_passed INTEGER NOT NULL,
  stars INTEGER NOT NULL,
  words_learned INTEGER NOT NULL,
  sentences_spoken INTEGER NOT NULL,
  streak INTEGER NOT NULL,
  level INTEGER,
  lesson_id TEXT,
  updated_at INTEGER NOT NULL
);

CREATE TABLE student_days (
  student_id INTEGER NOT NULL REFERENCES students(id),
  day INTEGER NOT NULL,
  minutes INTEGER NOT NULL,
  PRIMARY KEY (student_id, day)
) WITHOUT ROWID;

CREATE TABLE student_skills (
  student_id INTEGER NOT NULL REFERENCES students(id),
  skill TEXT NOT NULL,
  mastery REAL NOT NULL,
  attempts INTEGER NOT NULL,
  PRIMARY KEY (student_id, skill)
) WITHOUT ROWID;

-- Slows down anyone guessing join codes.
CREATE TABLE join_failures (
  ip TEXT PRIMARY KEY,
  failures INTEGER NOT NULL,
  window_start INTEGER NOT NULL
);
