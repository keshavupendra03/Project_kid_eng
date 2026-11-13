-- Textbook (syllabus) progress per chapter, and tests teachers set from questions already in the app.
-- Only phones whose parent agreed to consent version 2026-11-v4 send any of this.

-- One row per child per textbook chapter, e.g. chapter 'B7.04' = Class 7, Lesson 4.
CREATE TABLE student_chapters (
  student_id INTEGER NOT NULL REFERENCES students(id),
  chapter TEXT NOT NULL,
  sections_done INTEGER NOT NULL,
  sections_total INTEGER NOT NULL,
  stars INTEGER NOT NULL,
  -- Best stars in the chapter's own test in the app; NULL until taken.
  test_stars INTEGER,
  PRIMARY KEY (student_id, chapter)
) WITHOUT ROWID;

-- A test for one class. kind 'book' = textbook questions, 'course' = the app's own main course.
-- questions is a JSON list of [lessonId, stepId]; the question text lives in the app and in /catalog.
CREATE TABLE tests (
  id INTEGER PRIMARY KEY,
  school_id INTEGER NOT NULL REFERENCES schools(id),
  class_id INTEGER NOT NULL REFERENCES classes(id),
  created_by INTEGER NOT NULL REFERENCES users(id),
  kind TEXT NOT NULL CHECK (kind IN ('book', 'course')),
  title TEXT NOT NULL,
  lessons TEXT NOT NULL,
  questions TEXT NOT NULL,
  minutes INTEGER NOT NULL,
  opens_at INTEGER NOT NULL,
  closes_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE INDEX idx_tests_class ON tests(class_id, closes_at);
CREATE INDEX idx_tests_school ON tests(school_id, created_at);

-- One result per child per test. answers is a JSON list of {s: stepId, ok: 0|1, given: "what the child answered"}.
CREATE TABLE test_results (
  test_id INTEGER NOT NULL REFERENCES tests(id),
  student_id INTEGER NOT NULL REFERENCES students(id),
  score INTEGER NOT NULL,
  total INTEGER NOT NULL,
  seconds INTEGER NOT NULL,
  answers TEXT NOT NULL,
  submitted_at INTEGER NOT NULL,
  PRIMARY KEY (test_id, student_id)
) WITHOUT ROWID;
CREATE INDEX idx_test_results_student ON test_results(student_id);
