package com.samjhoenglish.learning

/** The rules of Word Power, kept free of Android so they can be unit-tested. */
object WordPower {
    /** Passing this many pack tests in a band opens the next band. */
    const val TESTS_TO_OPEN_NEXT = 8

    /** Placement questions per band that must be right (first try) to open that band straight away. */
    const val PLACEMENT_TO_OPEN = 5

    /** A pack test passes from 80%. */
    const val TEST_PASS = 0.8f

    /** Words learned before the monthly test appears, and how many questions it has. */
    const val MONTHLY_AFTER_WORDS = 20
    const val MONTHLY_QUESTIONS = 20

    /** A word is mastered once its test question has climbed to this review box (right first time, again and again). */
    const val MASTERED_BOX = 4

    /** The band for a child's class: [bands] are (minClass, maxClass) pairs, youngest first. */
    fun homeBand(classGrade: Int, bands: List<Pair<Int, Int>>): Int =
        bands.indexOfFirst { (min, max) -> classGrade in min..max }.takeIf { it >= 0 }
            ?: if (classGrade < (bands.firstOrNull()?.first ?: 0)) 0 else bands.lastIndex

    /**
     * Which bands are open: the child's own band and every band below it; a higher band opens
     * when [TESTS_TO_OPEN_NEXT] tests of the band below are passed, or when the placement test
     * shows the child already knows that band's words. So a younger child can climb as high as they can go.
     */
    fun openBands(home: Int, testsPassed: List<Int>, placementRight: List<Int>): Set<Int> {
        val open = (0..home).toMutableSet()
        for (band in home + 1 until testsPassed.size) {
            val belowDone = (band - 1) in open && testsPassed[band - 1] >= TESTS_TO_OPEN_NEXT
            val placed = placementRight.getOrElse(band) { 0 } >= PLACEMENT_TO_OPEN
            if (belowDone || placed) open += band
        }
        return open
    }

    /** Stars for a test: 3 from 90%, 2 (passed) from 80%, otherwise 1. */
    fun testStars(rightFirstTime: Int, questions: Int): Int {
        if (questions == 0) return 3
        val share = rightFirstTime.toFloat() / questions
        return when {
            share >= 0.9f -> 3
            share >= TEST_PASS -> 2
            else -> 1
        }
    }

    fun testPassed(stars: Int): Boolean = stars >= 2

    /** A pack opens once the pack before it has been learned. */
    fun packOpen(index: Int, learned: List<Boolean>): Boolean = index == 0 || learned.getOrElse(index - 1) { false }

    /** The word of the day: a different one each day, from words the child is learning. */
    fun <T> wordOfDay(candidates: List<T>, day: Long): T? =
        if (candidates.isEmpty()) null else candidates[(day % candidates.size).toInt()]

    /** The monthly test's id for a day number (days since 1970): one test per 30-day period. */
    fun monthlyId(day: Long): String = "WM.${day / 30}"
}
