package com.samjhoenglish.content

import com.samjhoenglish.i18n.L
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Shared JSON settings for content files (also used by the content tests). */
val ContentJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
}

/** assets/content/levels.json — the journey map. */
@Serializable
data class LevelsFile(val levels: List<LevelInfo>)

@Serializable
data class LevelInfo(
    val number: Int,
    val emoji: String,
    val title: L,
    val lessons: List<LessonRef> = emptyList(),
)

@Serializable
data class LessonRef(val id: String, val emoji: String, val title: L)

/** assets/content/placement.json — a short quiz per level, used to find where a new child starts. */
@Serializable
data class PlacementFile(val sections: List<PlacementSection>)

@Serializable
data class PlacementSection(val level: Int, val steps: List<Step>)

/** All lesson ids in learning order, across levels. */
val List<LevelInfo>.lessonOrder: List<String> get() = flatMap { level -> level.lessons.map { it.id } }

/** assets/content/lessons/<id>.json */
@Serializable
data class Lesson(val id: String, val level: Int, val steps: List<Step>)

/**
 * A word with its picture: "bus" + 🚌. [hindi] is the Hindi word in Devanagari (बस, सेब) and
 * [hinglish] the same in Roman letters (bas, seb), shown in Hinglish mode.
 */
@Serializable
data class Word(
    val english: String,
    val emoji: String = "",
    val hindi: String? = null,
    val hinglish: String? = null,
)

/** A Hindi meaning in both scripts. */
@Serializable
data class Meaning(val hi: String, val hinglish: String)

/**
 * A word in a sentence with its role, so Hindi and English words with the same job get the same colour:
 * s = who (subject), v = action/is-am-are (verb), o = the rest, q = question word, x = small helper words.
 */
@Serializable
data class RoleWord(val text: String, val role: String = "x")

@Serializable
data class HindiRoleWord(val hi: String, val hinglish: String, val role: String = "x")

/** One word of a Hindi sentence. [en] is set only for words that are really English (बस → bus). */
@Serializable
data class SentenceWord(val hi: String, val hinglish: String, val en: String? = null)

@Serializable
sealed interface Step {
    val id: String
    val skills: List<String>
}

/** Tinku explains something. No right or wrong answer. */
@Serializable
@SerialName("learn_card")
data class LearnCard(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val text: L,
    val word: Word? = null,
) : Step

/** Hear an English word, tap the matching picture. */
@Serializable
@SerialName("listen_tap")
data class ListenTap(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val answer: String,
    val options: List<Word>,
) : Step

/** See a picture, pick the matching written English word. */
@Serializable
@SerialName("word_pick")
data class WordPick(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val emoji: String,
    val answer: String,
    val options: List<String>,
) : Step

/** Tap every English word hiding inside a Hindi sentence. */
@Serializable
@SerialName("find_english")
data class FindEnglish(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val words: List<SentenceWord>,
) : Step

/** A letter (or letter team like "sh") and its recorded sound file in assets/audio/phonics. */
@Serializable
data class SoundPart(val letters: String, val sound: String)

/** Phonics card: the letter, its sound (recorded), a Hindi look-alike sound, and an example word. */
@Serializable
@SerialName("learn_sound")
data class LearnSound(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val letters: String,
    val sound: String,
    /** The nearest Hindi sound, e.g. "स" for s. */
    val hint: String,
    val text: L,
    val example: Word,
) : Step

/** Hear one of two look-alike words (vet / wet) and pick which one it was. */
@Serializable
@SerialName("minimal_pair")
data class MinimalPair(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val answer: String,
    val options: List<String>,
) : Step

/** Say a word aloud; the app listens and checks. */
@Serializable
@SerialName("say_word")
data class SayWord(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val word: Word,
    /** Other recogniser outputs that also count, e.g. "3" for "three". */
    val accept: List<String> = emptyList(),
) : Step

/** Say a whole sentence aloud. */
@Serializable
@SerialName("say_sentence")
data class SaySentence(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val sentence: String,
    val meaning: L,
    val accept: List<String> = emptyList(),
) : Step

/** Tap each letter to hear its sound (t-a-p), then say the whole word. */
@Serializable
@SerialName("blend")
data class Blend(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val word: String,
    val emoji: String,
    val parts: List<SoundPart>,
    val accept: List<String> = emptyList(),
) : Step

/** See an English word, pick its Hindi meaning. */
@Serializable
@SerialName("match_meaning")
data class MatchMeaning(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val word: Word,
    val answer: Meaning,
    val options: List<Meaning>,
) : Step

/** Hear a word, pick how it's written (for words with no picture, like "the" or "Monday"). */
@Serializable
@SerialName("listen_read")
data class ListenRead(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val answer: String,
    val options: List<String>,
) : Step

/**
 * Sentence Train: a Hindi sentence becomes an English train. Words keep their colour by role,
 * so the child sees the action word jump from the end (Hindi) to the middle (English).
 */
@Serializable
@SerialName("sentence_train")
data class SentenceTrain(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val hindi: List<HindiRoleWord>,
    val answer: List<RoleWord>,
    /** Extra wrong words mixed into the pool, e.g. "is" when the answer uses "am". */
    val extra: List<String> = emptyList(),
) : Step

/** Fill the gap: "She ___ happy." with is / am / are. */
@Serializable
@SerialName("fill_blank")
data class FillBlank(
    override val id: String,
    override val skills: List<String> = emptyList(),
    /** Contains "___" where the answer goes. */
    val sentence: String,
    val answer: String,
    val options: List<String>,
    val meaning: Meaning? = null,
) : Step

/** Mistake Detective: tap the wrong word in "She are happy". */
@Serializable
@SerialName("spot_mistake")
data class SpotMistake(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val words: List<String>,
    /** Index of the wrong word in [words]. */
    val wrong: Int,
    val fix: String,
    val meaning: Meaning? = null,
) : Step

const val BLANK = "___"

val FillBlank.filled: String get() = sentence.replace(BLANK, answer)

val SpotMistake.corrected: String
    get() = words.mapIndexed { i, w -> if (i == wrong) fix else w }.joinToString(" ")

val SentenceTrain.sentence: String get() = answer.joinToString(" ") { it.text }

val Step.isQuestion: Boolean get() = this !is LearnCard && this !is LearnSound

/** English the app says aloud after the question is answered or revealed. */
val Step.answerSpeech: String?
    get() = when (this) {
        is ListenTap -> answer
        is WordPick -> answer
        is FindEnglish -> words.mapNotNull { it.en }.joinToString(", ")
        is MinimalPair -> answer
        is SayWord -> word.english
        is SaySentence -> sentence
        is Blend -> word
        is MatchMeaning -> word.english
        is ListenRead -> answer
        is SentenceTrain -> sentence
        is FillBlank -> filled
        is SpotMistake -> corrected
        is LearnCard, is LearnSound -> null
    }
