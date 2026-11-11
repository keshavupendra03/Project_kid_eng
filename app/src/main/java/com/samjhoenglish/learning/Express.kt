package com.samjhoenglish.learning

import com.samjhoenglish.speech.SpeechChecker
import com.samjhoenglish.speech.WordMark

/** The rules of Tinku Express, kept free of Android so they can be unit-tested. */
object Express {
    /** A sentence counts as caught when this share of its words was heard. The listener misses some words, so not all are needed. */
    const val SENTENCE_CAUGHT = 0.6f

    /** Smart speed stays between these multiples of the train's speed. */
    const val MIN_SPEED = 0.6f
    const val MAX_SPEED = 1.4f

    /** The words of a passage and where each sentence starts and ends (by word index). */
    data class Passage(val words: List<String>, val sentences: List<IntRange>, val golden: Set<Int>)

    fun passage(text: String, golden: List<String> = emptyList()): Passage {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sentences = mutableListOf<IntRange>()
        var start = 0
        words.forEachIndexed { i, w ->
            if (w.trimEnd('"', '”', '’', '\'').lastOrNull() in setOf('.', '!', '?') || i == words.lastIndex) {
                sentences += start..i
                start = i + 1
            }
        }
        val goldenWords = golden.map(SpeechChecker::normalize).toSet()
        val goldenIndices = words.indices.filter { SpeechChecker.normalize(words[it]) in goldenWords }.toSet()
        return Passage(words, sentences, goldenIndices)
    }

    data class Score(
        val sentencesCaught: Int,
        val sentences: Int,
        val wordsRead: Int,
        val words: Int,
        val golden: Int,
        val goldenTotal: Int,
        val stars: Int,
        val failed: Boolean,
    )

    /**
     * Stars from the share of sentences caught: 3 from 90%, 2 from 70%, 1 from 40%.
     * With [lives], a round where more than half the sentences escaped is failed (no stars).
     * Golden words read cleanly (not a slip) add one star, up to three, if every one was read.
     */
    fun score(passage: Passage, marks: List<WordMark>, lives: Boolean): Score {
        val read = { i: Int -> marks.getOrNull(i) != null && marks[i] != WordMark.WAITING }
        val caught = passage.sentences.count { range -> range.count(read) >= SENTENCE_CAUGHT * (range.last - range.first + 1) }
        val share = if (passage.sentences.isEmpty()) 0f else caught.toFloat() / passage.sentences.size
        val failed = lives && share < 0.5f
        val golden = passage.golden.count { marks.getOrNull(it) == WordMark.READ }
        var stars = when {
            share >= 0.9f -> 3
            share >= 0.7f -> 2
            share >= 0.4f -> 1
            else -> 0
        }
        if (!failed && stars in 1..2 && passage.golden.isNotEmpty() && golden == passage.golden.size) stars++
        return Score(
            sentencesCaught = caught,
            sentences = passage.sentences.size,
            wordsRead = passage.words.indices.count(read),
            words = passage.words.size,
            golden = golden,
            goldenTotal = passage.golden.size,
            stars = if (failed) 0 else stars,
            failed = failed,
        )
    }

    /** Words read in a row, ending just before [next]: the combo meter. */
    fun streak(marks: List<WordMark>, next: Int): Int {
        var count = 0
        var i = minOf(next, marks.size) - 1
        while (i >= 0 && marks[i] == WordMark.READ) {
            count++
            i--
        }
        return count
    }

    /**
     * Smart speed: [behindLines] is how far the next word to read is above the reading line, in
     * lines (negative when it is still below, i.e. the child is ahead). Behind slows the train
     * quickly; ahead speeds it up slowly.
     */
    fun adaptSpeed(factor: Float, behindLines: Float, seconds: Float): Float {
        val change = when {
            behindLines > 0.5f -> -0.25f
            behindLines < -1.5f -> 0.06f
            else -> 0f
        }
        return (factor + change * seconds).coerceIn(MIN_SPEED, MAX_SPEED)
    }

    fun wordsPerMinute(words: Int, millis: Long): Int = if (millis <= 0 || words <= 0) 0 else (words * 60_000L / millis).toInt()

    /**
     * Which rounds are open. The first train is always open; each next train opens when two of the
     * previous train's passages (or all, if fewer) have 2 stars or more.
     */
    fun unlocked(levels: List<List<String>>, stars: Map<String, Int>): Set<String> {
        val open = mutableSetOf<String>()
        levels.forEachIndexed { i, passages ->
            val previous = levels.getOrNull(i - 1)
            val previousDone = previous == null || previous.count { (stars[it] ?: 0) >= 2 } >= minOf(2, previous.size)
            if (previousDone) open += passages
        }
        return open
    }
}
