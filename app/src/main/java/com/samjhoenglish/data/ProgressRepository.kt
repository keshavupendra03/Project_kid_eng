package com.samjhoenglish.data

import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.lessonOrder
import com.samjhoenglish.data.db.Attempt
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.data.db.ItemState
import com.samjhoenglish.data.db.LessonProgress
import com.samjhoenglish.data.db.ProgressDao
import com.samjhoenglish.data.db.SkillState
import com.samjhoenglish.data.db.StudySession
import com.samjhoenglish.learning.Learning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TimeZone

/** What one practice session contains: some revision questions first, then (usually) a new lesson. */
data class SessionPlan(
    /** The lesson being played, or null for a revision-only session. */
    val lessonId: String?,
    val steps: List<Step>,
    /** The first [reviewCount] steps are revision questions from earlier lessons. */
    val reviewCount: Int,
    /** Which lesson each step came from (revision steps come from other lessons). */
    val stepLessons: Map<String, String>,
)

class ProgressRepository(
    private val dao: ProgressDao,
    private val content: ContentRepository,
) {
    fun children(): Flow<List<Child>> = dao.children()

    suspend fun addChild(name: String, avatar: String, classGrade: Int): Long =
        dao.insertChild(Child(name = name, avatar = avatar, classGrade = classGrade, createdAt = now()))

    fun lessonStatus(childId: Long): Flow<Map<String, Learning.LessonStatus>> =
        dao.lessonProgress(childId).map { list -> list.associate { it.lessonId to it.toStatus() } }

    fun dueCount(childId: Long): Flow<Int> = dao.dueCount(childId, now())

    fun streak(childId: Long): Flow<Int> =
        dao.practiceDays(childId).map { Learning.streak(it.toSet(), localDay(now())) }

    /**
     * Records how a question went and updates the child's skills and review schedule.
     * [countsForReview] is false for the in-lesson second chance, so getting it right straight
     * after a mistake doesn't push the question far into the future.
     */
    suspend fun recordAnswer(
        childId: Long,
        lessonId: String,
        step: Step,
        outcome: Learning.Outcome,
        wrongTries: Int,
        mistakeSkill: String?,
        countsForReview: Boolean,
    ) {
        val time = now()
        dao.insertAttempt(
            Attempt(
                childId = childId,
                lessonId = lessonId,
                stepId = step.id,
                skills = step.skills.joinToString(","),
                correct = outcome == Learning.Outcome.FIRST_TRY || outcome == Learning.Outcome.AFTER_RETRY,
                firstTry = outcome == Learning.Outcome.FIRST_TRY,
                wrongTries = wrongTries,
                skipped = outcome == Learning.Outcome.SKIPPED,
                mistakeSkill = mistakeSkill,
                timestamp = time,
                day = localDay(time),
            ),
        )

        Learning.score(outcome)?.let { score ->
            val existing = dao.skills(childId).associateBy { it.skill }
            val scored = step.skills.associateWith { score }.toMutableMap()
            // A recognised pronunciation slip always counts against that sound.
            if (mistakeSkill != null) scored[mistakeSkill] = 0f
            scored.forEach { (skill, s) ->
                val old = existing[skill]
                dao.upsertSkill(
                    SkillState(
                        childId = childId,
                        skill = skill,
                        mastery = Learning.updatedMastery(old?.mastery, s),
                        attempts = (old?.attempts ?: 0) + 1,
                        updatedAt = time,
                    ),
                )
            }
        }

        if (countsForReview) {
            val box = Learning.nextBox(dao.item(childId, step.id)?.box ?: 1, outcome == Learning.Outcome.FIRST_TRY)
            dao.upsertItem(
                ItemState(
                    childId = childId,
                    stepId = step.id,
                    lessonId = lessonId,
                    box = box,
                    nextReviewAt = time + Learning.reviewGap(box),
                    lastSeenAt = time,
                ),
            )
        }
    }

    /** Saves the lesson result and returns the lesson the child should do next (may be the same one). */
    suspend fun finishLesson(childId: Long, lessonId: String, stars: Int): String? {
        val old = dao.lessonProgress(childId, lessonId)
        dao.upsertLessonProgress(
            LessonProgress(
                childId = childId,
                lessonId = lessonId,
                completed = true,
                bestStars = maxOf(stars, old?.bestStars ?: 0),
                timesPlayed = (old?.timesPlayed ?: 0) + 1,
                skippedByPlacement = old?.skippedByPlacement ?: false,
                updatedAt = now(),
            ),
        )
        return Learning.nextLesson(content.levels().lessonOrder, statusNow(childId))
    }

    /** Marks every lesson of the levels the child already knows, so they start at the right place. */
    suspend fun applyPlacement(childId: Long, knownLevels: Set<Int>) {
        content.levels().filter { it.number in knownLevels }.flatMap { it.lessons }.forEach { lesson ->
            dao.upsertLessonProgress(
                LessonProgress(
                    childId = childId,
                    lessonId = lesson.id,
                    completed = false,
                    bestStars = 0,
                    timesPlayed = 0,
                    skippedByPlacement = true,
                    updatedAt = now(),
                ),
            )
        }
        dao.markPlacementDone(childId)
    }

    /**
     * Builds a session. With [lessonId] it's just that lesson. Without, it's "today's task":
     * a few revision questions (due ones, weak skills first) and then the next lesson,
     * or a longer revision once every lesson is done.
     */
    suspend fun planSession(childId: Long, lessonId: String?): SessionPlan {
        if (lessonId != null) {
            val steps = content.lesson(lessonId).steps
            return SessionPlan(lessonId, steps, reviewCount = 0, stepLessons = steps.associate { it.id to lessonId })
        }

        val next = Learning.nextLesson(content.levels().lessonOrder, statusNow(childId))
        val index = content.stepIndex()
        val weak = dao.skills(childId).filter { Learning.isWeak(it.mastery, it.attempts) }.map { it.skill }.toSet()
        val candidates = dao.items(childId).mapNotNull { item ->
            val indexed = index[item.stepId] ?: return@mapNotNull null
            Learning.ReviewItem(item.stepId, item.lessonId, item.box, item.nextReviewAt, indexed.step.skills)
        }
        val reviewIds = Learning.pickReview(
            candidates,
            weak,
            now(),
            max = if (next != null) REVIEW_BEFORE_LESSON else REVIEW_ONLY,
            excludeLesson = next,
        )
        val review = reviewIds.mapNotNull { index[it] }
        val lessonSteps = next?.let { content.lesson(it).steps }.orEmpty()

        return SessionPlan(
            lessonId = next,
            steps = review.map { it.step } + lessonSteps,
            reviewCount = review.size,
            stepLessons = review.associate { it.step.id to it.lessonId } +
                lessonSteps.associate { it.id to next!! },
        )
    }

    suspend fun recordStudyTime(childId: Long, startAt: Long, endAt: Long) {
        dao.insertSession(StudySession(childId = childId, startAt = startAt, endAt = endAt, day = localDay(startAt)))
    }

    private suspend fun statusNow(childId: Long) =
        dao.lessonProgressNow(childId).associate { it.lessonId to it.toStatus() }

    private fun LessonProgress.toStatus() = Learning.LessonStatus(completed, bestStars, timesPlayed, skippedByPlacement)

    private fun now() = System.currentTimeMillis()

    private companion object {
        /** About 30% of a 15-question session. */
        const val REVIEW_BEFORE_LESSON = 5
        const val REVIEW_ONLY = 10
    }
}

/** Days since 1970 in the phone's time zone (works on Android 7, unlike java.time). */
fun localDay(millis: Long): Long = (millis + TimeZone.getDefault().getOffset(millis)) / (24 * 60 * 60 * 1000L)
