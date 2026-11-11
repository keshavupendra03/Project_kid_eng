package com.samjhoenglish.speech

enum class WordMark { WAITING, READ, SLIP }

/**
 * Follows a child reading a known paragraph aloud and decides which words they have read.
 *
 * Listeners report text per utterance: it grows and can change while the child speaks
 * (partial results), then is fixed (final). Each new guess is matched again from where the
 * utterance started, so a corrected guess fixes the colours; a final one is kept for good.
 *
 * Each utterance is lined up with the words ahead (best in-order match), so a listener that
 * missed some words doesn't leave the tracker stuck behind: a phrase heard further on is found.
 * A lone word may only skip a couple of words, so a common word like "a" can't jump ahead.
 * An Indian-English slip ("tree" for "three") counts as read, but is marked so Tinku can help.
 */
class ReadAlongTracker(val words: List<String>) {
    data class State(val marks: List<WordMark>, val slips: Map<Int, Mistake>, val next: Int)

    private val targets = words.map(SpeechChecker::normalize)
    private var committed = State(List(words.size) { WordMark.WAITING }, emptyMap(), 0)

    var state: State = committed
        private set

    val done: Boolean get() = state.next >= words.size

    fun hear(text: String, final: Boolean): State = hearAny(listOf(text), final)

    /** Words before [index] are gone (scrolled away in Tinku Express): stop waiting for them. */
    fun skipTo(index: Int) {
        val target = minOf(index, words.size)
        if (target <= committed.next) return
        committed = committed.copy(next = target)
        state = state.copy(next = maxOf(state.next, target))
    }

    /**
     * Several guesses for the same speech (a listener's top alternatives, best first). The first
     * guess is trusted; a later one is used only if it covers strictly more of the paragraph.
     * A slip counts as covered, so "we see tree birds" (the listener's best guess) isn't replaced
     * by the cleaner-looking "we see three birds" that it ranked second.
     */
    fun hearAny(guesses: List<String>, final: Boolean): State {
        var best = committed
        var bestCovered = -1
        for (guess in guesses) {
            val candidate = follow(guess)
            val covered = candidate.marks.count { it != WordMark.WAITING }
            if (covered > bestCovered) {
                best = candidate
                bestCovered = covered
            }
        }
        state = best
        if (final) committed = best
        return best
    }

    private fun follow(text: String): State {
        val tokens = SpeechChecker.normalize(text).split(" ").filter { it.isNotEmpty() }
        val start = committed.next
        val end = minOf(words.size, start + tokens.size + EXTRA_AHEAD)
        if (tokens.isEmpty() || start >= end) return committed

        val pairs = align(tokens, start, end)
        if (pairs.isEmpty()) return committed
        // A single heard word may not jump far: it is too easily a common word from further on.
        if (pairs.size == 1 && pairs.first().word - start >= LOOK_AHEAD) return committed

        val marks = committed.marks.toMutableList()
        val slips = committed.slips.toMutableMap()
        for (pair in pairs) {
            marks[pair.word] = pair.mark
            if (pair.mistake != null) slips[pair.word] = pair.mistake else slips.remove(pair.word)
        }
        return State(marks, slips, maxOf(pairs.last().word + 1, committed.next))
    }

    private class Pair(val word: Int, val mark: WordMark, val mistake: Mistake?)

    /** Best in-order match of heard tokens to words[start until end]; a clean match scores more than a slip. */
    private fun align(tokens: List<String>, start: Int, end: Int): List<Pair> {
        val n = tokens.size
        val m = end - start
        val judged = Array(n) { i -> Array(m) { j -> judge(targets[start + j], tokens[i]) } }
        val score = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) {
            for (j in 1..m) {
                val s = judged[i - 1][j - 1]?.first ?: 0
                score[i][j] = maxOf(score[i - 1][j], score[i][j - 1], if (s > 0) score[i - 1][j - 1] + s else 0)
            }
        }
        // Walk back preferring earlier paragraph words, so repeated words ("my ... my") match the nearest one.
        val pairs = ArrayList<Pair>()
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val judgement = judged[i - 1][j - 1]
            when {
                score[i][j - 1] == score[i][j] -> j--
                score[i - 1][j] == score[i][j] -> i--
                judgement != null -> {
                    pairs += Pair(start + j - 1, if (judgement.first == READ_SCORE) WordMark.READ else WordMark.SLIP, judgement.second)
                    i--
                    j--
                }
                else -> i--
            }
        }
        return pairs.reversed()
    }

    /**
     * (score, slip) when [token] counts as [target]: a clean match, or a known Indian-English slip.
     * Strict: a near miss like "pump" for "jump" is a misreading here, not listener noise.
     */
    private fun judge(target: String, token: String): kotlin.Pair<Int, Mistake?>? =
        when (val verdict = SpeechChecker.check(target, listOf(token), strict = true)) {
            SpeechVerdict.Correct -> READ_SCORE to null
            is SpeechVerdict.Wrong -> verdict.mistake?.let { SLIP_SCORE to it }
        }

    private companion object {
        /** How far a lone heard word may jump: lets the child (or the listener) skip up to two words. */
        const val LOOK_AHEAD = 3

        /** How many words beyond the utterance's own length a phrase is searched for. */
        const val EXTRA_AHEAD = 12
        const val READ_SCORE = 2
        const val SLIP_SCORE = 1
    }
}
