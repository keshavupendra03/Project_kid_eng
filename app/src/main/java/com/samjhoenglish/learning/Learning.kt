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
