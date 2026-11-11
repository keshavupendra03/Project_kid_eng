package com.samjhoenglish.data.db

import androidx.room.ColumnInfo
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
    /** Minutes a day this child may start lessons for; 0 means no limit. */
    @ColumnInfo(defaultValue = "0") val dailyLimitMinutes: Int = 0,
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

/** A real-life mission: the child says it's done (pending), then a parent approves it. */
@Entity(tableName = "mission_state", primaryKeys = ["childId", "missionId"])
data class MissionState(
    val childId: Long,
    val missionId: String,
    val approved: Boolean,
    val updatedAt: Long,
)

/** A child connected to their school after a parent's consent. */
@Entity(tableName = "school_links")
data class SchoolLink(
    @PrimaryKey val childId: Long,
    /** Proves this phone to the server. Stays in the app's private storage. */
    val deviceToken: String,
    val schoolName: String,
    val studentName: String,
    val grade: Int,
    val section: String,
    val linkedAt: Long,
    val lastSyncAt: Long = 0,
    /** Skills changed after this time still need sending. */
    val skillsSentUntil: Long = 0,
    /** What was last sent, so an unchanged summary isn't sent again. */
    val lastSummary: String = "",
)

/** One reading of a passage aloud: how much was read, how fast, and which sounds slipped. */
@Entity(tableName = "reading_results", indices = [Index("childId", "day")])
data class ReadingResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val lessonId: String,
    val wordsTotal: Int,
    /** Words read, including those with a pronunciation slip. */
    val wordsRead: Int,
    val slips: Int,
    val seconds: Int,
    val timestamp: Long,
    val day: Long,
)

/** One Tinku Express round: how much was caught and how fast the child read. */
@Entity(tableName = "express_results", indices = [Index("childId", "day")])
data class ExpressResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    /** X01.P1 … for trains, R1.P01 … for bonus re-reads of Reading stories. */
    val passageId: String,
    val stars: Int,
    val wordsRead: Int,
    val wordsTotal: Int,
    /** Reading speed, words per minute. */
    val wpm: Int,
    val bestStreak: Int,
    /** Fixed speed (Challenge) rather than Smart speed. */
    val challenge: Boolean,
    val timestamp: Long,
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
