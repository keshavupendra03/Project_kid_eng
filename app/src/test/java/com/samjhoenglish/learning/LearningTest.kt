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
    fun timeLimitCountsParentExtraTime() {
        val thirty = 30 * 60_000L
        assertEquals(false, Learning.timeIsUp(thirty, limitMinutes = 0, extraMinutes = 0))
        assertEquals(true, Learning.timeIsUp(thirty, limitMinutes = 30, extraMinutes = 0))
        assertEquals(false, Learning.timeIsUp(thirty, limitMinutes = 30, extraMinutes = 15))
    }

    @Test
    fun weakestSkillsPutSpecificOnesFirst() {
        val skills = listOf(
            Learning.SkillScore("speak.word", 0.1f, 9),
            Learning.SkillScore("sound.th", 0.4f, 5),
            Learning.SkillScore("grammar.i_am", 0.2f, 4),
            Learning.SkillScore("vocab.food", 0.1f, 2), // too few tries to judge
            Learning.SkillScore("vocab.body", 0.8f, 9), // strong
        )
        assertEquals(listOf("grammar.i_am", "sound.th", "speak.word"), Learning.weakestSkills(skills, broad = setOf("speak.word")))
    }

    @Test
    fun lastDaysFillsGapsAndRounds() {
        val days = Learning.lastDays(mapOf(10L to 90_000L, 8L to 20_000L), today = 10, count = 3)
        assertEquals(listOf(8L to 0, 9L to 0, 10L to 2), days)
    }

    @Test
    fun missionsFollowLessonsAndApprovals() {
        val missions = listOf("a" to 0, "b" to 2, "c" to 5)
        assertEquals("a", Learning.currentMission(missions, emptySet(), 0))
        assertNull(Learning.currentMission(missions, setOf("a"), 1))
        assertEquals("b", Learning.currentMission(missions, setOf("a"), 3))
    }

    @Test
    fun tinkuPiecesUnlockWithStars() {
        assertEquals(emptySet<Learning.TinkuPiece>(), Learning.unlockedPieces(9))
        assertEquals(setOf(Learning.TinkuPiece.CAP, Learning.TinkuPiece.BOW_TIE), Learning.unlockedPieces(40))
        assertEquals(Learning.TinkuPiece.GLASSES, Learning.nextPiece(40))
        assertNull(Learning.nextPiece(160))
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        assertEquals(3, Learning.streak(setOf(10L, 9L, 8L, 5L), today = 10))
        assertEquals(3, Learning.streak(setOf(9L, 8L, 7L), today = 10))
        assertEquals(0, Learning.streak(setOf(7L), today = 10))
    }

    @Test
    fun firstReadingLevelIsOpenAndTheNextOpensAfterFourPassages() {
        val levels = listOf(listOf("R1.P01", "R1.P02", "R1.P03", "R1.P04", "R1.P05", "R1.P06"), listOf("R2.P01", "R2.P02"))
        assertEquals(levels[0].toSet(), Learning.unlockedPassages(levels, emptyMap()))
        val three = mapOf("R1.P01" to done(3), "R1.P02" to done(2), "R1.P03" to done(2))
        assertEquals(levels[0].toSet(), Learning.unlockedPassages(levels, three))
        val four = three + ("R1.P05" to done(2))
        assertEquals((levels[0] + levels[1]).toSet(), Learning.unlockedPassages(levels, four))
        // One star doesn't count as passed on the first go, but does after two tries.
        assertEquals(levels[0].toSet(), Learning.unlockedPassages(levels, three + ("R1.P04" to done(1))))
        assertEquals((levels[0] + levels[1]).toSet(), Learning.unlockedPassages(levels, three + ("R1.P04" to done(1, times = 2))))
    }

    @Test
    fun nextPassageIsTheFirstOpenOneNotPassed() {
        val levels = listOf(listOf("R1.P01", "R1.P02"), listOf("R2.P01"))
        assertEquals("R1.P01", Learning.nextPassage(levels, emptyMap()))
        assertEquals("R1.P02", Learning.nextPassage(levels, mapOf("R1.P01" to done(3))))
        assertEquals("R2.P01", Learning.nextPassage(levels, mapOf("R1.P01" to done(3), "R1.P02" to done(2))))
        assertNull(Learning.nextPassage(levels, mapOf("R1.P01" to done(3), "R1.P02" to done(2), "R2.P01" to done(2))))
    }

    @Test
    fun aPassageIsPassedFromSeventyPercentOfWordsRead() {
        assertEquals(true, Learning.readingPassed(wordsRead = 31, wordsTotal = 43))
        assertEquals(false, Learning.readingPassed(wordsRead = 30, wordsTotal = 43))
        assertEquals(false, Learning.readingPassed(wordsRead = 0, wordsTotal = 0))
    }
}
