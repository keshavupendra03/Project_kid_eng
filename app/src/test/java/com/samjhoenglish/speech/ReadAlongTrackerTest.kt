package com.samjhoenglish.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadAlongTrackerTest {
    // 9 words: I have a red ball. We see three birds.
    private val paragraph = "I have a red ball. We see three birds.".split(" ")

    private fun ReadAlongTracker.marks() = state.marks.joinToString("") {
        when (it) {
            WordMark.WAITING -> "."
            WordMark.READ -> "R"
            WordMark.SLIP -> "S"
        }
    }

    @Test
    fun wordsTurnGreenAsTheGuessGrows() {
        val t = ReadAlongTracker(paragraph)
        t.hear("i have", final = false)
        assertEquals("RR.......", t.marks())
        t.hear("i have a red ball", final = false)
        assertEquals("RRRRR....", t.marks())
        assertEquals(5, t.state.next)
    }

    @Test
    fun aCorrectedGuessFixesTheColours() {
        val t = ReadAlongTracker(paragraph)
        t.hear("i hat", final = false)
        assertEquals("R........", t.marks())
        t.hear("i have", final = false)
        assertEquals("RR.......", t.marks())
    }

    @Test
    fun readingCarriesOnAcrossPauses() {
        val t = ReadAlongTracker(paragraph)
        t.hear("i have a red ball", final = true)
        t.hear("we see", final = false)
        assertEquals("RRRRRRR..", t.marks())
    }

    @Test
    fun aSkippedWordDoesNotBlockTheRest() {
        val t = ReadAlongTracker(paragraph)
        t.hear("i have red ball", final = true)
        assertEquals("RR.RR....", t.marks())
        assertEquals(5, t.state.next)
    }

    @Test
    fun noiseAndRepeatsAreIgnored() {
        val t = ReadAlongTracker(paragraph)
        t.hear("um i i have uh a", final = true)
        assertEquals("RRR......", t.marks())
    }

    @Test
    fun indianSlipCountsAsReadButIsMarked() {
        val t = ReadAlongTracker(paragraph)
        t.hear("i have a red ball we see tree birds", final = true)
        assertEquals("RRRRRRRSR", t.marks())
        assertEquals(SpeechChecker.SKILL_TH, t.state.slips.getValue(7).skill)
        assertTrue(t.done)
    }

    @Test
    fun theAlternativeThatFitsTheParagraphBestWins() {
        val t = ReadAlongTracker(paragraph)
        t.hearAny(listOf("i half red", "i have a red", "eye have"), final = true)
        assertEquals("RRRR.....", t.marks())
    }

    /** What the listener really heard on 11 Nov: it missed "I have a", which used to leave the tracker stuck at word 1. */
    @Test
    fun catchesUpWhenTheListenerMissesTheFirstWords() {
        val easy = ("I have a red ball. It is very big. I play with my ball in the park. My friend Ravi plays with me. " +
            "We run and we jump. Then we sit under a tree. We see three birds. It is a happy day.").split(" ")
        val heard = listOf(
            listOf("ball", "red ball"), listOf("it is very big"), listOf("play we", "play with"), listOf("my ball"),
            listOf("in the park", "in park", "park"), listOf("my friend ravi plays with me"),
            listOf("we run and we jump", "a we run and we jump"), listOf("then we sit under a tree"),
            listOf("we see three birds"), listOf("it is a happy day", "it is a a happy day"),
        )
        val t = ReadAlongTracker(easy)
        heard.forEach { t.hearAny(it, final = true) }
        assertEquals(39, t.state.marks.count { it == WordMark.READ })
        assertTrue(t.done)
    }

    @Test
    fun aLoneCommonWordCannotJumpAhead() {
        val t = ReadAlongTracker(paragraph)
        t.hear("i have", final = true)
        t.hear("birds", final = true)
        assertEquals("RR.......", t.marks())
    }

    @Test
    fun repeatedWordsMatchTheNearestOne() {
        val t = ReadAlongTracker("my ball and my bat".split(" "))
        t.hear("my", final = true)
        assertEquals("R....", t.marks())
    }

    @Test
    fun indianSlipVersionsAreOfferedOnlyForMeaningfulWords() {
        assertTrue("tree" in SpeechChecker.slipVariants("three"))
        assertTrue("wan" in SpeechChecker.slipVariants("van"))
        assertTrue("tank" in SpeechChecker.slipVariants("Thank"))
        assertEquals(emptyList<String>(), SpeechChecker.slipVariants("with"))
        assertEquals(emptyList<String>(), SpeechChecker.slipVariants("the"))
        assertEquals(emptyList<String>(), SpeechChecker.slipVariants("red"))
        assertTrue("free" !in SpeechChecker.slipVariants("three"))
    }

    /** What the phone's listener reported on 11 Nov when "tree" was said on purpose: the slip was its first guess. */
    @Test
    fun theListenersFirstGuessIsNotOverriddenByACleanerSecondGuess() {
        val t = ReadAlongTracker("we see three birds".split(" "))
        t.hearAny(listOf("we see tree birds", "we see three birds"), final = true)
        assertEquals("RRSR", t.marks())
    }

    @Test
    fun aLaterGuessIsUsedOnlyWhenItCoversMoreWords() {
        val t = ReadAlongTracker("we see three birds".split(" "))
        t.hearAny(listOf("we see", "we see three birds"), final = true)
        assertEquals("RRRR", t.marks())
    }

    /** Said on purpose on 11 Nov: "pump" for jump and "sleepy" for happy must not turn green. */
    @Test
    fun aNearMissIsAMisreadingNotAMatch() {
        val t = ReadAlongTracker("we run and we jump it is a happy day".split(" "))
        t.hear("we run and we pump it is a sleepy day", final = true)
        assertEquals("RRRR.RRR.R", t.marks())
    }

    @Test
    fun aHeardSlipVersionTurnsTheWordYellow() {
        val t = ReadAlongTracker("we see three birds".split(" "))
        t.hear("we see tree birds", final = true)
        assertEquals("RRSR", t.marks())
    }

    @Test
    fun listenersAreToldEachSentenceAndItsSlipVersions() {
        val versions = ReadAlongHints.sentenceVersions("We see three birds. It is a happy day.")
        assertTrue(listOf("we", "see", "three", "birds") in versions)
        assertTrue(listOf("we", "see", "tree", "birds") in versions)
        assertTrue(listOf("it", "is", "a", "happy", "day") in versions)
        assertEquals(listOf("tree", "dree"), ReadAlongHints.slipWords("We see three birds."))
        // A listener that has no spelling for "dree" isn't told about it.
        assertEquals(listOf("tree"), ReadAlongHints.slipWords("We see three birds.") { it != "dree" })
    }

    @Test
    fun recogniserNumbersAndLettersStillCount() {
        val t = ReadAlongTracker("we see three birds".split(" "))
        t.hear("we c 3 birds", final = true)
        assertEquals("RRRR", t.marks())
    }
}
