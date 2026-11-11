package com.samjhoenglish.content

import com.samjhoenglish.i18n.L
import kotlinx.serialization.Serializable

/** assets/content/wordpower.json — vocabulary by class band, in packs of ten words. */
@Serializable
data class WordPowerFile(val placement: String, val bands: List<WordBand>)

/** Words for one class band, e.g. Class 5–6. */
@Serializable
data class WordBand(
    val id: String,
    val emoji: String,
    val title: L,
    val minClass: Int,
    val maxClass: Int,
    val packs: List<WordPack>,
)

/** Ten words: [id] is the lesson that teaches them, [testId] the pack test. */
@Serializable
data class WordPack(
    val id: String,
    val testId: String,
    val emoji: String,
    val title: L,
    val words: List<PowerWord>,
)

@Serializable
data class PowerWord(
    val english: String,
    val emoji: String,
    val hindi: String,
    val hinglish: String,
    val meaning: String,
    val example: String,
    val exampleMeaning: Meaning? = null,
)

/** Word Power lessons: W34.P01 (learn), W34.P01T (test), W.PLACE (placement), WM.<month> (monthly test). */
val String.isWordPower: Boolean get() = startsWith("W")

/** Tests are marked strictly: 8 out of 10 passes. */
val String.isWordTest: Boolean get() = (startsWith("W") && endsWith("T") && contains(".P")) || startsWith(WORD_MONTHLY_PREFIX)

/** Lesson ids the app understands but that have no file: a review of due words, and this month's test. */
const val WORD_REVIEW = "W.REVIEW"
const val WORD_PLACEMENT = "W.PLACE"
const val WORD_MONTHLY = "W.MONTHLY"
const val WORD_MONTHLY_PREFIX = "WM."
