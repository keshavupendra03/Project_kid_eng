package com.samjhoenglish.speech

import com.samjhoenglish.speech.SpeechChecker.check
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechCheckerTest {

    private fun assertCorrect(target: String, vararg heard: String, accept: List<String> = emptyList()) {
        assertEquals("'$target' heard as ${heard.toList()}", SpeechVerdict.Correct, check(target, heard.toList(), accept))
    }

    private fun assertSwap(skill: String, target: String, vararg heard: String) {
        val verdict = check(target, heard.toList())
        assertTrue("'$target' heard as ${heard.toList()} should be wrong, was $verdict", verdict is SpeechVerdict.Wrong)
        assertEquals(skill, (verdict as SpeechVerdict.Wrong).mistake?.skill)
    }

    private fun assertWrongNoTip(target: String, vararg heard: String) {
        val verdict = check(target, heard.toList())
        assertTrue(verdict is SpeechVerdict.Wrong)
        assertEquals(null, (verdict as SpeechVerdict.Wrong).mistake)
    }

    @Test
    fun exactAndFormattingDifferencesAreCorrect() {
        assertCorrect("bus", "Bus")
        assertCorrect("bus", "bus.")
        assertCorrect("TV", "t.v.")
        assertCorrect("ice cream", "icecream")
        assertCorrect("Thank you", "thank you")
    }

    @Test
    fun shortWordsHeardAsLettersOrHomophonesAreCorrect() {
        assertCorrect("eye", "I")
        assertCorrect("tea", "T")
        assertCorrect("the", "di")
        assertCorrect("key", "K")
        assertCorrect("bee", "B")
        assertCorrect("two", "2")
        assertCorrect("four", "for")
        assertCorrect("sun", "son")
        assertCorrect("I see the sea", "i see the c")
    }

    @Test
    fun soundAlikesDoNotHideRealSwaps() {
        // "tree" is not a sound-alike of "three": that's the th habit we teach.
        assertSwap(SpeechChecker.SKILL_TH, "three", "tree")
        assertWrongNoTip("tea", "tin")
    }

    @Test
    fun acceptListCounts() {
        assertCorrect("three", "3", accept = listOf("3"))
    }

    @Test
    fun hindiSpeakerSwapsGetTips() {
        assertSwap(SpeechChecker.SKILL_TH, "thin", "tin")
        assertSwap(SpeechChecker.SKILL_TH, "three", "tree")
        assertSwap(SpeechChecker.SKILL_TH, "this", "dis")
        assertSwap(SpeechChecker.SKILL_V, "van", "ban")
        assertSwap(SpeechChecker.SKILL_V, "vet", "wet")
        assertSwap(SpeechChecker.SKILL_W, "wet", "vet")
        assertSwap(SpeechChecker.SKILL_Z, "zoo", "joo")
        assertSwap(SpeechChecker.SKILL_F, "fan", "pan")
        assertSwap(SpeechChecker.SKILL_SH, "ship", "sip")
        assertSwap(SpeechChecker.SKILL_S_CLUSTER, "school", "is school")
    }

    @Test
    fun swapInsideSentenceIsFound() {
        assertSwap(SpeechChecker.SKILL_TH, "Thank you", "tank you")
        assertSwap(SpeechChecker.SKILL_V, "I am very happy", "I am wary happy")
    }

    @Test
    fun swapBeatsLaterCorrectGuess() {
        // Best guess is the swap; we still want to teach the sound.
        assertSwap(SpeechChecker.SKILL_TH, "thin", "tin", "thin")
    }

    @Test
    fun correctLowerGuessIsAcceptedWhenBestIsNotASwap() {
        assertCorrect("cricket", "critic", "cricket")
    }

    @Test
    fun smallAccentSlipsAreForgiven() {
        assertCorrect("happy", "happi")
        assertCorrect("chocolate", "chocolat")
        assertCorrect("I am a good student", "I am good student")
    }

    @Test
    fun differentWordsAreWrong() {
        assertWrongNoTip("bus", "boss")
        assertWrongNoTip("pen", "pin")
        assertWrongNoTip("doctor", "banana")
        assertWrongNoTip("bus")
    }
}
