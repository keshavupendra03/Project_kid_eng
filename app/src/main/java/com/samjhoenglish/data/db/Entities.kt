package com.samjhoenglish.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One child using the app. Several children can share a phone. */
@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String,
    /** School class 1–8. Class 1–4 get the junior look, 5–8 the senior look. */
    val classGrade: Int,
    val createdAt: Long,
    val placementDone: Boolean = false,
) {
    val isJunior: Boolean get() = classGrade <= 4
}

@Entity(tableName = "lesson_progress", primaryKeys = ["childId", "lessonId"])
data class LessonProgress(
    val childId: Long,
    val lessonId: String,
    val completed: Boolean,
    val bestStars: Int,
    val timesPlayed: Int,
    /** The placement test showed the child already knows this lesson. */
    val skippedByPlacement: Boolean,
    val updatedAt: Long,
)

/** How strong a child is in one skill (e.g. "sound.th"), from 0 to 1. */
@Entity(tableName = "skill_state", primaryKeys = ["childId", "skill"])
data class SkillState(
    val childId: Long,
    val skill: String,
    val mastery: Float,
    val attempts: Int,
    val updatedAt: Long,
)

/** Spaced-review schedule for one question (Leitner box). */
@Entity(tableName = "item_state", primaryKeys = ["childId", "stepId"])
data class ItemState(
    val childId: Long,
    val stepId: String,
    val lessonId: String,
    val box: Int,
    val nextReviewAt: Long,
    val lastSeenAt: Long,
)

/** One answered question. This is the raw history behind the parent dashboard. */
@Entity(tableName = "attempts", indices = [Index("childId", "day")])
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val lessonId: String,
    val stepId: String,
    /** Comma-separated skill ids. */
    val skills: String,
    val correct: Boolean,
    val firstTry: Boolean,
    val wrongTries: Int,
    val skipped: Boolean,
    /** The pronunciation slip recognised, e.g. "sound.th". */
    val mistakeSkill: String?,
    val timestamp: Long,
    /** Local calendar day (days since 1970), for streaks and daily charts. */
    val day: Long,
)

/** Time the app was open for a child. */
@Entity(tableName = "study_sessions", indices = [Index("childId", "day")])
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val startAt: Long,
    val endAt: Long,
    val day: Long,
)
