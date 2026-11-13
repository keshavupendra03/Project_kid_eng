package com.samjhoenglish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    // Children
    @Query("SELECT * FROM children ORDER BY createdAt")
    fun children(): Flow<List<Child>>

    @Insert
    suspend fun insertChild(child: Child): Long

    @Query("UPDATE children SET placementDone = 1 WHERE id = :childId")
    suspend fun markPlacementDone(childId: Long)

    // Lessons
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId")
    fun lessonProgress(childId: Long): Flow<List<LessonProgress>>

    @Query("SELECT * FROM lesson_progress WHERE childId = :childId")
    suspend fun lessonProgressNow(childId: Long): List<LessonProgress>

    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND lessonId = :lessonId")
    suspend fun lessonProgress(childId: Long, lessonId: String): LessonProgress?

    @Upsert
    suspend fun upsertLessonProgress(progress: LessonProgress)

    @Query("SELECT COALESCE(SUM(bestStars), 0) FROM lesson_progress WHERE childId = :childId")
    fun lessonStars(childId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM mission_state WHERE childId = :childId AND approved = 1")
    fun approvedMissionCount(childId: Long): Flow<Int>

    // Skills
    @Query("SELECT * FROM skill_state WHERE childId = :childId")
    suspend fun skills(childId: Long): List<SkillState>

    @Upsert
    suspend fun upsertSkill(state: SkillState)

    // Spaced review
    @Query("SELECT * FROM item_state WHERE childId = :childId")
    suspend fun items(childId: Long): List<ItemState>

    @Query("SELECT COUNT(*) FROM item_state WHERE childId = :childId AND nextReviewAt <= :now")
    fun dueCount(childId: Long, now: Long): Flow<Int>

    @Query("SELECT * FROM item_state WHERE childId = :childId AND stepId = :stepId")
    suspend fun item(childId: Long, stepId: String): ItemState?

    @Upsert
    suspend fun upsertItem(state: ItemState)

    // History
    @Insert
    suspend fun insertAttempt(attempt: Attempt)

    @Query("SELECT DISTINCT day FROM attempts WHERE childId = :childId ORDER BY day DESC LIMIT 400")
    fun practiceDays(childId: Long): Flow<List<Long>>

    @Insert
    suspend fun insertSession(session: StudySession)

    // Parent section
    @Query("UPDATE children SET dailyLimitMinutes = :minutes WHERE id = :childId")
    suspend fun setDailyLimit(childId: Long, minutes: Int)

    @Query("SELECT day, SUM(endAt - startAt) AS millis FROM study_sessions WHERE childId = :childId AND day >= :fromDay GROUP BY day")
    fun dailyMillis(childId: Long, fromDay: Long): Flow<List<DayMillis>>

    @Query("SELECT COALESCE(SUM(endAt - startAt), 0) FROM study_sessions WHERE childId = :childId AND day = :day")
    suspend fun millisOn(childId: Long, day: Long): Long

    @Query("SELECT DISTINCT stepId FROM attempts WHERE childId = :childId AND correct = 1")
    fun correctSteps(childId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM attempts WHERE childId = :childId AND correct = 1 AND skills LIKE '%speak.sentence%'")
    fun sentencesSpoken(childId: Long): Flow<Int>

    @Query("SELECT * FROM skill_state WHERE childId = :childId")
    fun skillsFlow(childId: Long): Flow<List<SkillState>>

    @Query("SELECT * FROM mission_state WHERE childId = :childId")
    fun missions(childId: Long): Flow<List<MissionState>>

    @Upsert
    suspend fun upsertMission(state: MissionState)

    @Query("DELETE FROM mission_state WHERE childId = :childId AND missionId = :missionId")
    suspend fun deleteMission(childId: Long, missionId: String)

    // Reading
    @Insert
    suspend fun insertReadingResult(result: ReadingResult)

    @Query("SELECT * FROM reading_results WHERE childId = :childId ORDER BY timestamp DESC LIMIT :limit")
    fun readingResults(childId: Long, limit: Int): Flow<List<ReadingResult>>

    // Only the Reading ladder's stories (R1.P01 …), not textbook poems read aloud.
    @Query("SELECT COUNT(DISTINCT lessonId) FROM reading_results WHERE childId = :childId AND lessonId LIKE 'R%'")
    fun passagesRead(childId: Long): Flow<Int>

    // Word Power
    /** Words whose test question has climbed to [box] or higher: answered right first time again and again. */
    @Query("SELECT COUNT(*) FROM item_state WHERE childId = :childId AND lessonId LIKE 'W%T' AND box >= :box")
    fun wordsMastered(childId: Long, box: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM item_state WHERE childId = :childId AND lessonId LIKE 'W%T' AND nextReviewAt <= :now")
    fun wordsDue(childId: Long, now: Long): Flow<Int>

    @Query("SELECT * FROM attempts WHERE childId = :childId AND lessonId = :lessonId")
    fun attemptsFor(childId: Long, lessonId: String): Flow<List<Attempt>>

    // Tinku Express
    @Insert
    suspend fun insertExpressResult(result: ExpressResult)

    /** Fastest reading speed in a round where most of the passage was read. */
    @Query("SELECT MAX(wpm) FROM express_results WHERE childId = :childId AND wordsRead * 2 >= wordsTotal")
    fun expressBestWpm(childId: Long): Flow<Int?>

    @Query("SELECT MAX(wpm) FROM express_results WHERE childId = :childId AND passageId = :passageId AND wordsRead * 2 >= wordsTotal")
    suspend fun expressBestWpmFor(childId: Long, passageId: String): Int?

    @Query("DELETE FROM express_results WHERE childId = :childId")
    suspend fun deleteExpressResults(childId: Long)

    // School
    @Query("SELECT * FROM school_links WHERE childId = :childId")
    fun schoolLink(childId: Long): Flow<SchoolLink?>

    @Query("SELECT * FROM school_links WHERE childId = :childId")
    suspend fun schoolLinkNow(childId: Long): SchoolLink?

    @Query("SELECT * FROM school_links")
    suspend fun schoolLinks(): List<SchoolLink>

    @Upsert
    suspend fun upsertSchoolLink(link: SchoolLink)

    @Query("DELETE FROM school_links WHERE childId = :childId")
    suspend fun deleteSchoolLink(childId: Long)

    // School tests
    @Query("SELECT * FROM school_tests WHERE childId = :childId AND testId = :testId")
    suspend fun schoolTest(childId: Long, testId: Long): SchoolTest?

    @Query("SELECT * FROM school_tests WHERE childId = :childId")
    suspend fun schoolTestsNow(childId: Long): List<SchoolTest>

    /** Tests still to take: open, not handed in on this phone or another. */
    @Query(
        """SELECT t.* FROM school_tests t LEFT JOIN test_attempts a ON a.childId = t.childId AND a.testId = t.testId
           WHERE t.childId = :childId AND t.closesAt > :now AND t.doneOnServer = 0 AND a.submittedAt IS NULL
           ORDER BY t.closesAt""",
    )
    fun activeTests(childId: Long, now: Long): Flow<List<SchoolTest>>

    @Upsert
    suspend fun upsertSchoolTests(tests: List<SchoolTest>)

    @Query("DELETE FROM school_tests WHERE childId = :childId AND testId NOT IN (:keep)")
    suspend fun deleteSchoolTestsExcept(childId: Long, keep: List<Long>)

    @Query("UPDATE school_tests SET notified = 1 WHERE childId = :childId AND testId IN (:ids)")
    suspend fun markTestsNotified(childId: Long, ids: List<Long>)

    @Query("SELECT * FROM test_attempts WHERE childId = :childId AND testId = :testId")
    suspend fun testAttempt(childId: Long, testId: Long): TestAttempt?

    /** Handed-in tests, newest first: the child's and the parent's result list. */
    @Query("SELECT * FROM test_attempts WHERE childId = :childId AND submittedAt IS NOT NULL ORDER BY submittedAt DESC")
    fun testResults(childId: Long): Flow<List<TestAttempt>>

    @Query("SELECT * FROM test_attempts WHERE submittedAt IS NOT NULL AND uploaded = 0")
    suspend fun testsToUpload(): List<TestAttempt>

    @Upsert
    suspend fun upsertTestAttempt(attempt: TestAttempt)

    @Query("DELETE FROM school_tests WHERE childId = :childId")
    suspend fun deleteSchoolTests(childId: Long)

    @Query("DELETE FROM test_attempts WHERE childId = :childId")
    suspend fun deleteTestAttempts(childId: Long)

    @Query("SELECT * FROM skill_state WHERE childId = :childId AND updatedAt > :since")
    suspend fun skillsChangedSince(childId: Long, since: Long): List<SkillState>

    @Query("SELECT COALESCE(SUM(bestStars), 0) FROM lesson_progress WHERE childId = :childId")
    suspend fun lessonStarsNow(childId: Long): Int

    @Query("SELECT COUNT(*) FROM mission_state WHERE childId = :childId AND approved = 1")
    suspend fun approvedMissionCountNow(childId: Long): Int

    @Query("SELECT DISTINCT day FROM attempts WHERE childId = :childId ORDER BY day DESC LIMIT 400")
    suspend fun practiceDaysNow(childId: Long): List<Long>

    @Query("SELECT day, SUM(endAt - startAt) AS millis FROM study_sessions WHERE childId = :childId AND day >= :fromDay GROUP BY day")
    suspend fun dailyMillisNow(childId: Long, fromDay: Long): List<DayMillis>

    @Query("SELECT DISTINCT stepId FROM attempts WHERE childId = :childId AND correct = 1")
    suspend fun correctStepsNow(childId: Long): List<String>

    @Query("SELECT COUNT(*) FROM attempts WHERE childId = :childId AND correct = 1 AND skills LIKE '%speak.sentence%'")
    suspend fun sentencesSpokenNow(childId: Long): Int

    // Deleting a child removes everything stored about them.
    @Query("DELETE FROM children WHERE id = :childId")
    suspend fun deleteChildRow(childId: Long)

    @Query("DELETE FROM lesson_progress WHERE childId = :childId")
    suspend fun deleteLessonProgress(childId: Long)

    @Query("DELETE FROM skill_state WHERE childId = :childId")
    suspend fun deleteSkills(childId: Long)

    @Query("DELETE FROM item_state WHERE childId = :childId")
    suspend fun deleteItems(childId: Long)

    @Query("DELETE FROM attempts WHERE childId = :childId")
    suspend fun deleteAttempts(childId: Long)

    @Query("DELETE FROM study_sessions WHERE childId = :childId")
    suspend fun deleteSessions(childId: Long)

    @Query("DELETE FROM mission_state WHERE childId = :childId")
    suspend fun deleteMissions(childId: Long)

    @Query("DELETE FROM reading_results WHERE childId = :childId")
    suspend fun deleteReadingResults(childId: Long)

    @Transaction
    suspend fun deleteChildCompletely(childId: Long) {
        deleteLessonProgress(childId)
        deleteSkills(childId)
        deleteItems(childId)
        deleteAttempts(childId)
        deleteSessions(childId)
        deleteMissions(childId)
        deleteSchoolLink(childId)
        deleteReadingResults(childId)
        deleteExpressResults(childId)
        deleteSchoolTests(childId)
        deleteTestAttempts(childId)
        deleteChildRow(childId)
    }
}

/** Total study time on one calendar day. */
data class DayMillis(val day: Long, val millis: Long)
