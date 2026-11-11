package com.samjhoenglish.speech

/** Which reply in a conversation the child gave. */
sealed interface ReplyMatch {
    data class Matched(val replyIndex: Int) : ReplyMatch
    data class NoMatch(val mistake: Mistake?) : ReplyMatch
}

/**
 * Works out which scripted reply the child said. Patterns may use "*" for any words ("my name is *").
 *
 * A strict word-by-word pass runs first, so replies that differ by one word ("I want a pen" and
 * "I want a pencil") lead to the right branch. Only then does the lenient checker get a chance.
 */
object ReplyMatcher {
    private const val WILDCARD = "*"
    private const val MAX_WILDCARD_WORDS = 5
    private val fillers = setOf("please", "sir", "madam", "maam", "mam", "um", "uh", "ok", "okay")

    fun match(patternsByReply: List<List<String>>, guesses: List<String>): ReplyMatch {
        val heard = guesses.map(::words).filter { it.isNotEmpty() }
        for (guess in heard) {
            patternsByReply.forEachIndexed { reply, patterns ->
                if (patterns.any { matches(pattern(it), guess) }) return ReplyMatch.Matched(reply)
            }
        }

        var mistake: Mistake? = null
        patternsByReply.forEachIndexed { reply, patterns ->
            patterns.filter { WILDCARD !in it }.forEach { pattern ->
                when (val verdict = SpeechChecker.check(pattern, guesses)) {
                    SpeechVerdict.Correct -> return ReplyMatch.Matched(reply)
                    is SpeechVerdict.Wrong -> mistake = mistake ?: verdict.mistake
                }
            }
        }
        return ReplyMatch.NoMatch(mistake)
    }

    private fun words(text: String): List<String> =
        SpeechChecker.normalize(text).split(" ").filter { it.isNotEmpty() && it !in fillers }

    private fun pattern(text: String): List<String> =
        text.split(" ").flatMap { if (it == WILDCARD) listOf(WILDCARD) else words(it) }

    private fun matches(pattern: List<String>, heard: List<String>): Boolean {
        if (pattern.isEmpty()) return heard.isEmpty()
        if (pattern.first() == WILDCARD) {
            return (1..minOf(MAX_WILDCARD_WORDS, heard.size)).any { matches(pattern.drop(1), heard.drop(it)) }
        }
        return heard.isNotEmpty() && SpeechChecker.wordsAlike(pattern.first(), heard.first()) &&
            matches(pattern.drop(1), heard.drop(1))
    }
}
