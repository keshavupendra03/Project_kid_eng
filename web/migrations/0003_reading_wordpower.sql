-- Reading ladder, Tinku Express and Word Power progress, sent by app versions from Nov 2026.
-- Older phones leave these empty, and the website shows "–" for them.
ALTER TABLE student_summary ADD COLUMN reading_level INTEGER;
ALTER TABLE student_summary ADD COLUMN passages_read INTEGER;
ALTER TABLE student_summary ADD COLUMN reading_accuracy INTEGER;
ALTER TABLE student_summary ADD COLUMN words_per_minute INTEGER;
ALTER TABLE student_summary ADD COLUMN express_best_wpm INTEGER;
ALTER TABLE student_summary ADD COLUMN power_words_learned INTEGER;
ALTER TABLE student_summary ADD COLUMN power_words_mastered INTEGER;
ALTER TABLE student_summary ADD COLUMN monthly_test_stars INTEGER;
