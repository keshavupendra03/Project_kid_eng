package com.samjhoenglish.data

import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.ReadAlong
import com.samjhoenglish.content.Roleplay
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.StoryQuestion
import com.samjhoenglish.content.WORD_MONTHLY
import com.samjhoenglish.content.WORD_MONTHLY_PREFIX
import com.samjhoenglish.content.WORD_REVIEW
import com.samjhoenglish.content.isBookLesson
import com.samjhoenglish.content.isExpressRound
import com.samjhoenglish.content.nextSection
import com.samjhoenglish.content.isReadingLesson
import com.samjhoenglish.content.isWordPower
import com.samjhoenglish.content.isWordTest
import com.samjhoenglish.content.lessonOrder
import com.samjhoenglish.data.db.Attempt
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.data.db.ItemState
import com.samjhoenglish.data.db.LessonProgress
import com.samjhoenglish.data.db.ProgressDao
import com.samjhoenglish.data.db.ExpressResult
import com.samjhoenglish.data.db.ReadingResult
import com.samjhoenglish.data.db.SkillState
import com.samjhoenglish.data.db.StudySession
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.learning.SkillCatalog
import com.samjhoenglish.learning.StuckHelp
import com.samjhoenglish.learning.WordPower
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    /** Steps copied exactly from the school textbook. */
    val bookSteps: Set<String> = emptySet(),
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

    /** Lesson stars plus one for every mission a parent approved. Unlocks Tinku's outfits. */
    fun totalStars(childId: Long): Flow<Int> =
        combine(dao.lessonStars(childId), dao.approvedMissionCount(childId)) { stars, missions -> stars + missions }

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

        // A passage is too long to be a revision question, and a story question means nothing without it.
        // Textbook chapters are practised from their own screen, not in the daily revision.
        if (countsForReview && step !is ReadAlong && step !is StoryQuestion && !lessonId.isBookLesson) {
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
        val status = statusNow(childId)
        return when {
            lessonId.isWordPower -> wordPowerNext(lessonId, stars)
            lessonId.isReadingLesson -> Learning.nextPassage(readingOrder(), status)
            // A section with 1 star asks for another go, like the journey; otherwise on to the next section.
            lessonId.isBookLesson -> if (stars <= 1) lessonId else content.book().nextSection(lessonId)
            else -> Learning.nextLesson(content.levels().lessonOrder, status)
        }
    }

    /** Passage ids per reading level, in order. */
    suspend fun readingOrder(): List<List<String>> = content.readingLevels().map { level -> level.lessons.map { it.id } }

    /** Saves how a passage was read, for the parent's reading row and the school summary. */
    suspend fun recordReading(childId: Long, lessonId: String, wordsTotal: Int, wordsRead: Int, slips: Int, seconds: Int) {
        val time = now()
        dao.insertReadingResult(
            ReadingResult(
                childId = childId, lessonId = lessonId, wordsTotal = wordsTotal, wordsRead = wordsRead,
                slips = slips, seconds = seconds, timestamp = time, day = localDay(time),
            ),
        )
    }

    fun readingResults(childId: Long, limit: Int = 10): Flow<List<ReadingResult>> = dao.readingResults(childId, limit)

    fun passagesRead(childId: Long): Flow<Int> = dao.passagesRead(childId)

    /**
     * Saves a Tinku Express round. Train rounds also keep their best stars with the lessons (so they
     * count towards Tinku's outfits); bonus re-reads of Reading stories only keep the speed.
     * Returns the child's previous best speed for this passage, to show "new best".
     */
    suspend fun finishExpress(
        childId: Long, passageId: String, stars: Int, wordsRead: Int, wordsTotal: Int,
        wpm: Int, bestStreak: Int, challenge: Boolean,
    ): Int? {
        val previousBest = dao.expressBestWpmFor(childId, passageId)
        val time = now()
        dao.insertExpressResult(
            ExpressResult(
                childId = childId, passageId = passageId, stars = stars, wordsRead = wordsRead, wordsTotal = wordsTotal,
                wpm = wpm, bestStreak = bestStreak, challenge = challenge, timestamp = time, day = localDay(time),
            ),
        )
        if (passageId.isExpressRound) {
            val old = dao.lessonProgress(childId, passageId)
            dao.upsertLessonProgress(
                LessonProgress(
                    childId = childId,
                    lessonId = passageId,
                    completed = true,
                    bestStars = maxOf(stars, old?.bestStars ?: 0),
                    timesPlayed = (old?.timesPlayed ?: 0) + 1,
                    skippedByPlacement = false,
                    updatedAt = time,
                ),
            )
        }
        return previousBest
    }

    fun expressBestWpm(childId: Long): Flow<Int?> = dao.expressBestWpm(childId)

    fun wordsMastered(childId: Long): Flow<Int> = dao.wordsMastered(childId, WordPower.MASTERED_BOX)

    fun wordsDue(childId: Long): Flow<Int> = dao.wordsDue(childId, now())

    fun attemptsFor(childId: Long, lessonId: String): Flow<List<Attempt>> = dao.attemptsFor(childId, lessonId)

    suspend fun expressBestWpmFor(childId: Long, passageId: String): Int? = dao.expressBestWpmFor(childId, passageId)

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
        val plan = when {
            lessonId == WORD_REVIEW -> wordReview(childId)
            lessonId == WORD_MONTHLY || lessonId?.startsWith(WORD_MONTHLY_PREFIX) == true -> monthlyTest(childId)
            else -> basicSession(childId, lessonId)
        }
        val played = plan.lessonId?.let { statusNow(childId)[it]?.timesPlayed ?: 0 } ?: 0
        // Revision questions have already been taught. A lesson counts once the child has tried it.
        val relevant = plan.steps.take(plan.reviewCount).flatMap { it.skills } +
            if (played >= 1) plan.steps.drop(plan.reviewCount).flatMap { it.skills } else emptyList()
        val scores = dao.skills(childId).map { Learning.SkillScore(it.skill, it.mastery, it.attempts) }
        val stuck = Learning.stuckSkills(relevant.toSet(), scores, SkillCatalog.broad)
        val adapted = StuckHelp.adapt(plan.steps, plan.reviewCount, stuck)
        return plan.copy(steps = adapted.steps, reviewCount = adapted.reviewCount)
    }

    /** Word Bank review: word test questions that are due, the least-known first. */
    private suspend fun wordReview(childId: Long): SessionPlan {
        val index = content.wordTestIndex()
        val due = dao.items(childId)
            .filter { it.lessonId.isWordTest && it.nextReviewAt <= now() && it.stepId in index }
            .sortedWith(compareBy({ it.box }, { it.nextReviewAt }))
            .take(WORD_REVIEW_SIZE)
            .mapNotNull { index[it.stepId] }
        return SessionPlan(null, due.map { it.step }, reviewCount = due.size, stepLessons = due.associate { it.step.id to it.lessonId })
    }

    /** This month's Word Power test: questions about words from every pack the child has learned. */
    private suspend fun monthlyTest(childId: Long): SessionPlan {
        val status = statusNow(childId)
        val learned = content.wordPower().bands.flatMap { it.packs }.filter { status[it.id]?.completed == true }
        val index = content.wordTestIndex()
        val day = localDay(now())
        val pool = learned.flatMap { pack -> index.values.filter { it.lessonId == pack.testId } }
            .shuffled(kotlin.random.Random(day / 30))
            .take(WordPower.MONTHLY_QUESTIONS)
        return SessionPlan(WordPower.monthlyId(day), pool.map { it.step }, reviewCount = 0, stepLessons = pool.associate { it.step.id to it.lessonId })
    }

    /** After a Word Power lesson: learning leads to its test; a passed test leads to the next pack; a failed one asks for another go. */
    private suspend fun wordPowerNext(lessonId: String, stars: Int): String? {
        val packs = content.wordPower().bands.flatMap { band -> band.packs.map { band.id to it } }
        val at = packs.indexOfFirst { (_, pack) -> pack.id == lessonId || pack.testId == lessonId }
        if (at < 0) return null
        val (band, pack) = packs[at]
        return when {
            lessonId == pack.id -> pack.testId
            !WordPower.testPassed(stars) -> lessonId
            else -> packs.getOrNull(at + 1)?.takeIf { it.first == band }?.second?.id
        }
    }

    private suspend fun basicSession(childId: Long, lessonId: String?): SessionPlan {
        if (lessonId != null) {
            val lesson = content.lesson(lessonId)
            val steps = lesson.steps
            return SessionPlan(lessonId, steps, reviewCount = 0, stepLessons = steps.associate { it.id to lessonId }, bookSteps = lesson.bookSteps.toSet())
        }

        val next = Learning.nextLesson(content.levels().lessonOrder, statusNow(childId))
        val index = content.stepIndex()
        val weak = dao.skills(childId).filter { Learning.isWeak(it.mastery, it.attempts) }.map { it.skill }.toSet()
        val candidates = dao.items(childId).mapNotNull { item ->
            val indexed = index[item.stepId] ?: return@mapNotNull null
            // A whole conversation is too long to be one revision question.
            if (indexed.step is Roleplay) return@mapNotNull null
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
        const val WORD_REVIEW_SIZE = 12
    }
}

/** Days since 1970 in the phone's time zone (works on Android 7, unlike java.time). */
fun localDay(millis: Long): Long = (millis + TimeZone.getDefault().getOffset(millis)) / (24 * 60 * 60 * 1000L)
