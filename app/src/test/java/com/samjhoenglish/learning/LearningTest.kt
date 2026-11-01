package com.samjhoenglish.learning

import com.samjhoenglish.learning.Learning.LessonStatus
import com.samjhoenglish.learning.Learning.ReviewItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LearningTest {

    @Test
    fun boxesMoveUpOnFirstTryAndResetOnMistakes() {
        assertEquals(2, Learning.nextBox(1, firstTry = true))
        assertEquals(Learning.MAX_BOX, Learning.nextBox(Learning.MAX_BOX, firstTry = true))
        assertEquals(1, Learning.nextBox(4, firstTry = false))
    }

    @Test
    fun masteryFollowsRecentAnswers() {
        var m = Learning.updatedMastery(null, 1f)
        assertEquals(1f, m, 0.001f)
        repeat(3) { m = Learning.updatedMastery(m, 0f) }
        assertEquals(0.343f, m, 0.001f)
        assertNull(Learning.score(Learning.Outcome.SKIPPED))
    }

    @Test
    fun starsFromFirstTryScore() {
        assertEquals(3, Learning.stars(9, 10))
        assertEquals(2, Learning.stars(6, 10))
        assertEquals(1, Learning.stars(5, 10))
        assertEquals(3, Learning.stars(0, 0))
    }

    private val order = listOf("A", "B", "C")
    private fun done(stars: Int, times: Int = 1) = LessonStatus(completed = true, bestStars = stars, timesPlayed = times, skippedByPlacement = false)

    @Test
    fun lessonsUnlockInOrderWhenPassed() {
        assertEquals(setOf("A"), Learning.unlockedLessons(order, emptyMap()))
        assertEquals("A", Learning.nextLesson(order, emptyMap()))
        assertEquals(setOf("A", "B"), Learning.unlockedLessons(order, mapOf("A" to done(2))))
        assertEquals("B", Learning.nextLesson(order, mapOf("A" to done(3))))
    }

    @Test
    fun oneStarNeedsASecondTryButNeverBlocksForever() {
        assertEquals("A", Learning.nextLesson(order, mapOf("A" to done(1, times = 1))))
        assertEquals("B", Learning.nextLesson(order, mapOf("A" to done(1, times = 2))))
    }

    @Test
    fun placementSkipsLessons() {
        val skipped = LessonStatus(completed = false, bestStars = 0, timesPlayed = 0, skippedByPlacement = true)
        assertEquals("C", Learning.nextLesson(order, mapOf("A" to skipped, "B" to skipped)))
        assertNull(Learning.nextLesson(order, order.associateWith { done(3) }))
    }

    @Test
    fun reviewPrefersWeakSkillsThenLowBoxes() {
        val now = 1_000L
        val items = listOf(
            ReviewItem("s1", "A", box = 3, nextReviewAt = 500, skills = listOf("vocab")),
            ReviewItem("s2", "A", box = 1, nextReviewAt = 900, skills = listOf("vocab")),
            ReviewItem("s3", "B", box = 4, nextReviewAt = 5_000, skills = listOf("sound.th")), // not due, but weak
            ReviewItem("s4", "B", box = 2, nextReviewAt = 9_000, skills = listOf("vocab")), // not due
            ReviewItem("s5", "C", box = 1, nextReviewAt = 100, skills = listOf("vocab")), // excluded lesson
        )
        val picked = Learning.pickReview(items, weakSkills = setOf("sound.th"), now = now, max = 3, excludeLesson = "C")
        assertEquals(listOf("s3", "s2", "s1"), picked)
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        assertEquals(3, Learning.streak(setOf(10L, 9L, 8L, 5L), today = 10))
        assertEquals(3, Learning.streak(setOf(9L, 8L, 7L), today = 10))
        assertEquals(0, Learning.streak(setOf(7L), today = 10))
    }
}
