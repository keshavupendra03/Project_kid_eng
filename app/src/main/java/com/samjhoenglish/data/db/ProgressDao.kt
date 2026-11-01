package com.samjhoenglish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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
}
