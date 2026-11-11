package com.samjhoenglish.learning

/**
 * The adaptive-learning rules, kept free of Android so they can be unit-tested.
 * See PLAN.md section 9.
 */
object Learning {
    private const val DAY = 24 * 60 * 60 * 1000L

    // ---------- Spaced review (Leitner boxes) ----------

    const val MAX_BOX = 5

    /** How long until a question in this box comes back. Box 1 returns in the very next session. */
    fun reviewGap(box: Int): Long = when (box) {
        1 -> 0L
        2 -> 1 * DAY
        3 -> 3 * DAY
        4 -> 7 * DAY
        else -> 16 * DAY
    }

    /** Right first time moves a question up a box; anything else sends it back to box 1. */
    fun nextBox(box: Int, firstTry: Boolean): Int = if (firstTry) (box + 1).coerceAtMost(MAX_BOX) else 1

    // ---------- Skill mastery ----------

    enum class Outcome { FIRST_TRY, AFTER_RETRY, MISSED, SKIPPED }

    /** How much an answer says about the skill. Skipped speaking says nothing, so it doesn't count. */
    fun score(outcome: Outcome): Float? = when (outcome) {
        Outcome.FIRST_TRY -> 1f
        Outcome.AFTER_RETRY -> 0.5f
        Outcome.MISSED -> 0f
        Outcome.SKIPPED -> null
    }

    /** Recent answers count more: each new answer moves mastery 30% of the way towards its score. */
    fun updatedMastery(current: Float?, score: Float): Float =
        if (current == null) score else current * 0.7f + score * 0.3f

    const val WEAK_BELOW = 0.5f
    private const val MIN_ATTEMPTS_FOR_WEAK = 3

    fun isWeak(mastery: Float, attempts: Int) = attempts >= MIN_ATTEMPTS_FOR_WEAK && mastery < WEAK_BELOW

    // ---------- Lessons ----------

    fun stars(rightFirstTime: Int, questions: Int): Int {
        if (questions == 0) return 3
        val score = rightFirstTime.toFloat() / questions
        return when {
            score >= 0.9f -> 3
            score >= 0.6f -> 2
            else -> 1
        }
    }

    data class LessonStatus(
        val completed: Boolean,
        val bestStars: Int,
        val timesPlayed: Int,
        val skippedByPlacement: Boolean,
    )

    private const val PASS_STARS = 2

    /**
     * A lesson is passed with 2+ stars. After two tries we let the child move on anyway —
     * spaced review keeps bringing the hard questions back, so nobody gets stuck forever.
     */
    fun isPassed(status: LessonStatus?): Boolean = status != null &&
        (status.skippedByPlacement || (status.completed && (status.bestStars >= PASS_STARS || status.timesPlayed >= 2)))

    /** Lessons open one after another, in [order] (all lessons of all levels). */
    fun unlockedLessons(order: List<String>, status: Map<String, LessonStatus>): Set<String> =
        order.filterIndexed { i, id ->
            i == 0 || isPassed(status[order[i - 1]]) || status[id]?.completed == true || status[id]?.skippedByPlacement == true
        }.toSet()

    /** The first open lesson the child hasn't passed yet, or null when everything is done. */
    fun nextLesson(order: List<String>, status: Map<String, LessonStatus>): String? {
        val unlocked = unlockedLessons(order, status)
        return order.firstOrNull { it in unlocked && !isPassed(status[it]) }
    }

    // ---------- Reading ladder ----------

    /** A reading level opens once this many passages of the level before it are passed. */
    const val PASSAGES_TO_OPEN_NEXT = 4

    /**
     * Which passages are open. The first level is always open; each later level opens when
     * [PASSAGES_TO_OPEN_NEXT] passages of the previous level are passed (or all of them, if fewer).
     */
    fun unlockedPassages(levels: List<List<String>>, status: Map<String, LessonStatus>): Set<String> {
        val open = mutableSetOf<String>()
        levels.forEachIndexed { i, passages ->
            val previous = levels.getOrNull(i - 1)
            val previousDone = previous == null ||
                previous.count { isPassed(status[it]) } >= minOf(PASSAGES_TO_OPEN_NEXT, previous.size)
            if (previousDone) open += passages
        }
        return open
    }

    /** The next passage to read: the first open one not yet passed. */
    fun nextPassage(levels: List<List<String>>, status: Map<String, LessonStatus>): String? {
        val open = unlockedPassages(levels, status)
        return levels.flatten().firstOrNull { it in open && !isPassed(status[it]) }
    }

    /** Words read correctly out of all words: 3 stars from 90%, 2 from 70%, else 1. The passage counts as passed from 70%. */
    const val READING_PASS = 0.7f

    fun readingPassed(wordsRead: Int, wordsTotal: Int): Boolean = wordsTotal > 0 && wordsRead.toFloat() / wordsTotal >= READING_PASS

    // ---------- Review selection ----------

    data class ReviewItem(val stepId: String, val lessonId: String, val box: Int, val nextReviewAt: Long, val skills: List<String>)

    /**
     * Picks questions to revise: due ones first, weak skills before strong ones, lowest box first.
     * Questions from [excludeLesson] are skipped because the child is about to play that lesson anyway.
     */
    fun pickReview(
        items: List<ReviewItem>,
        weakSkills: Set<String>,
        now: Long,
        max: Int,
        excludeLesson: String? = null,
    ): List<String> = items
        .filter { it.lessonId != excludeLesson }
        .filter { it.nextReviewAt <= now || it.skills.any { s -> s in weakSkills } }
        .sortedWith(
            compareByDescending<ReviewItem> { it.skills.any { s -> s in weakSkills } }
                .thenBy { it.box }
                .thenBy { it.nextReviewAt },
        )
        .take(max)
        .map { it.stepId }

    // ---------- Placement ----------

    const val PLACEMENT_PASS = 0.8f

    // ---------- Parent section ----------

    /** A limit of 0 means no limit. Extra minutes are what a parent granted today with the PIN. */
    fun timeIsUp(spentMillis: Long, limitMinutes: Int, extraMinutes: Int): Boolean =
        limitMinutes > 0 && spentMillis >= (limitMinutes + extraMinutes) * 60_000L

    data class SkillScore(val skill: String, val mastery: Float, val attempts: Int)

    /** The weakest skills. Specific ones ("is/am/are") come before broad ones ("speaking words"), which say less. */
    fun weakestSkills(skills: List<SkillScore>, broad: Set<String>, max: Int = 3): List<String> = skills
        .filter { isWeak(it.mastery, it.attempts) }
        .sortedWith(compareBy<SkillScore> { it.skill in broad }.thenBy { it.mastery })
        .take(max)
        .map { it.skill }

    /** Skills from [skills] the child is still weak in, weakest first. Broad ones can't be explained in one card. */
    fun stuckSkills(skills: Collection<String>, scores: List<SkillScore>, broad: Set<String>, max: Int = 2): List<String> =
        weakestSkills(scores.filter { it.skill in skills && it.skill !in broad }, broad, max)

    /** Minutes for each of the last [count] days, oldest first; days with no study are 0. */
    fun lastDays(millisByDay: Map<Long, Long>, today: Long, count: Int = 7): List<Pair<Long, Int>> =
        ((today - count + 1)..today).map { day -> day to (((millisByDay[day] ?: 0L) + 30_000L) / 60_000L).toInt() }

    /** The first mission the child has earned (by lessons passed) that a parent hasn't approved yet. */
    fun currentMission(missions: List<Pair<String, Int>>, approved: Set<String>, lessonsPassed: Int): String? =
        missions.firstOrNull { (id, need) -> need <= lessonsPassed && id !in approved }?.first

    // ---------- Rewards ----------

    /** Things Tinku can wear, earned with stars. */
    enum class TinkuPiece(val stars: Int) { CAP(10), BOW_TIE(40), GLASSES(90), CROWN(160) }

    fun unlockedPieces(stars: Int): Set<TinkuPiece> = TinkuPiece.entries.filter { stars >= it.stars }.toSet()

    fun nextPiece(stars: Int): TinkuPiece? = TinkuPiece.entries.firstOrNull { stars < it.stars }

    // ---------- Streak ----------

    /** Days in a row with practice, counting back from today (or yesterday, so the streak survives until bedtime). */
    fun streak(practiceDays: Set<Long>, today: Long): Int {
        var day = if (today in practiceDays) today else today - 1
        var count = 0
        while (day in practiceDays) {
            count++
            day--
        }
        return count
    }
}
