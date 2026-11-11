package com.samjhoenglish.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyMatcherTest {
    private val shop = listOf(
        listOf("i want a pencil", "a pencil"),
        listOf("i want a pen", "a pen"),
        listOf("i want *"),
    )

    @Test
    fun branchesOnTheExactWord() {
        assertEquals(ReplyMatch.Matched(0), ReplyMatcher.match(shop, listOf("I want a pencil")))
        assertEquals(ReplyMatch.Matched(1), ReplyMatcher.match(shop, listOf("I want a pen")))
        assertEquals(ReplyMatch.Matched(2), ReplyMatcher.match(shop, listOf("I want a mango")))
    }

    @Test
    fun wildcardTakesAnyName() {
        val name = listOf(listOf("my name is *", "i am *"))
        assertEquals(ReplyMatch.Matched(0), ReplyMatcher.match(name, listOf("my name is Aarav Kumar")))
        assertEquals(ReplyMatch.Matched(0), ReplyMatcher.match(name, listOf("I'm Riya")))
        assertTrue(ReplyMatcher.match(name, listOf("Riya")) is ReplyMatch.NoMatch)
    }

    @Test
    fun politeWordsDoNotGetInTheWay() {
        assertEquals(ReplyMatch.Matched(0), ReplyMatcher.match(shop, listOf("I want a pencil please")))
    }

    @Test
    fun laterGuessesCount() {
        assertEquals(ReplyMatch.Matched(1), ReplyMatcher.match(shop, listOf("banana", "a pen")))
    }

    @Test
    fun pronunciationSlipGivesATip() {
        val thanks = listOf(listOf("thank you"))
        val result = ReplyMatcher.match(thanks, listOf("tank you"))
        assertEquals(SpeechChecker.SKILL_TH, (result as ReplyMatch.NoMatch).mistake?.skill)
    }

    @Test
    fun contractionsAreExpanded() {
        assertEquals(SpeechVerdict.Correct, SpeechChecker.check("I am happy", listOf("I'm happy")))
        assertEquals(SpeechVerdict.Correct, SpeechChecker.check("It is not cold", listOf("it isn't cold")))
        assertEquals(SpeechVerdict.Correct, SpeechChecker.check("She is my sister", listOf("she's my sister")))
    }
}
