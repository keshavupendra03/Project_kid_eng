package com.samjhoenglish.learning

import com.samjhoenglish.content.ContentJson
import com.samjhoenglish.content.ExpressFile
import com.samjhoenglish.speech.ReadAlongTracker
import com.samjhoenglish.speech.WordMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExpressTest {
    private val pip = Express.passage("Pip is a pup. Pip can run. Pip has a red ball. Good pup, Pip!", listOf("run", "ball"))

    private fun marks(text: String) = text.map {
        when (it) {
            'R' -> WordMark.READ
            'S' -> WordMark.SLIP
            else -> WordMark.WAITING
        }
    }

    @Test
    fun passageKnowsItsSentencesAndGoldenWords() {
        assertEquals(listOf(0..3, 4..6, 7..11, 12..14), pip.sentences)
        assertEquals(setOf(6, 11), pip.golden)
    }

    @Test
    fun aSentenceIsCaughtFromSixtyPercentOfItsWords() {
        // Sentence 1: 3 of 4 read (caught). Sentence 2: 1 of 3 (not). Sentence 3: all. Sentence 4: none.
        val score = Express.score(pip, marks("RRR.R..RRRRR..."), lives = false)
        assertEquals(2, score.sentencesCaught)
        assertEquals(1, score.stars)
    }

    @Test
    fun allGoldenWordsReadCleanlyAddAStar() {
        val noGolden = Express.score(pip, marks("RRRRRR.RRRRS..."), lives = false)
        val golden = Express.score(pip, marks("RRRRRRRRRRRR..."), lives = false)
        assertEquals(2, noGolden.stars)
        assertEquals(3, golden.stars)
        assertEquals(2, golden.golden)
    }

    @Test
    fun fromTrainFourARoundFailsWhenMostSentencesEscape() {
        val score = Express.score(pip, marks("RRRR..........."), lives = true)
        assertTrue(score.failed)
        assertEquals(0, score.stars)
        assertEquals(false, Express.score(pip, marks("RRRR..........."), lives = false).failed)
    }

    @Test
    fun comboCountsWordsReadInARow() {
        assertEquals(3, Express.streak(marks("RR.RRR"), next = 6))
        assertEquals(0, Express.streak(marks("RRS"), next = 3))
    }

    @Test
    fun smartSpeedSlowsWhenBehindAndSpeedsUpWhenAhead() {
        assertTrue(Express.adaptSpeed(1f, behindLines = 1f, seconds = 1f) < 1f)
        assertTrue(Express.adaptSpeed(1f, behindLines = -2f, seconds = 1f) > 1f)
        assertEquals(1f, Express.adaptSpeed(1f, behindLines = 0f, seconds = 1f))
        assertEquals(Express.MIN_SPEED, Express.adaptSpeed(0.61f, behindLines = 3f, seconds = 5f))
    }

    @Test
    fun theNextTrainOpensAfterTwoPassagesWithTwoStars() {
        val levels = listOf(listOf("X01.P1", "X01.P2", "X01.P3"), listOf("X02.P1"))
        assertEquals(setOf("X01.P1", "X01.P2", "X01.P3"), Express.unlocked(levels, mapOf("X01.P1" to 3, "X01.P2" to 1)))
        assertTrue("X02.P1" in Express.unlocked(levels, mapOf("X01.P1" to 3, "X01.P2" to 2)))
    }

    @Test
    fun scrolledAwayWordsStopHoldingTheReaderBack() {
        val t = ReadAlongTracker(pip.words)
        t.hear("pip is a pup", final = true)
        t.skipTo(7)
        t.hear("pip has a red ball", final = true)
        assertEquals(12, t.state.next)
    }

    @Test
    fun wordsPerMinute() {
        assertEquals(60, Express.wordsPerMinute(30, 30_000))
        assertEquals(0, Express.wordsPerMinute(0, 30_000))
    }

    /** Ten trains, three passages each, getting faster and longer; every golden word is in its passage. */
    @Test
    fun expressContentIsWellFormed() {
        val levels = ContentJson.decodeFromString<ExpressFile>(File("src/main/assets/content/express.json").readText()).levels
        val problems = mutableListOf<String>()
        if (levels.size != 10) problems += "expected 10 trains, found ${levels.size}"
        levels.zipWithNext { a, b -> if (b.wpm <= a.wpm) problems += "train ${b.number} is not faster than ${a.number}" }
        val ids = mutableSetOf<String>()
        levels.forEach { level ->
            if (level.passages.size != 3) problems += "train ${level.number}: needs 3 passages"
            if (level.lives != (level.number >= 4)) problems += "train ${level.number}: lives should start at train 4"
            level.passages.forEach { p ->
                if (!ids.add(p.id)) problems += "${p.id}: duplicate id"
                if (!p.id.startsWith("X%02d.".format(level.number))) problems += "${p.id}: id should start with X%02d.".format(level.number)
                if (Regex("\\d").containsMatchIn(p.text)) problems += "${p.id}: digits (the listener hears numbers as words)"
                if (p.title.hi.none { it in 'ऀ'..'ॿ' }) problems += "${p.id}: Hindi title has no Devanagari"
                val passage = Express.passage(p.text, p.golden)
                if (passage.golden.size < p.golden.size) problems += "${p.id}: a golden word is not in the text"
                if (passage.sentences.size < 3) problems += "${p.id}: needs at least 3 sentences"
            }
            val longest = level.passages.maxOf { Express.passage(it.text).words.size }
            val limit = 15 + level.number * 7
            if (longest > limit) problems += "train ${level.number}: a passage has $longest words, keep it under $limit"
        }
        assertTrue("Express problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
