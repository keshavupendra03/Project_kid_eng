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

/** assets/content/missions.json — small English tasks for real life, in order. */
@Serializable
data class MissionsFile(val missions: List<Mission>)

/** [needLessons] is how many lessons the child must have passed before this mission appears. */
@Serializable
data class Mission(val id: String, val emoji: String, val needLessons: Int, val text: L)

/** All lesson ids in learning order, across levels. */
val List<LevelInfo>.lessonOrder: List<String> get() = flatMap { level -> level.lessons.map { it.id } }

/**
 * assets/content/lessons/<id>.json. [bookSteps] are questions copied exactly from the school textbook
 * (they can come in the exam), shown with a "Book question" badge.
 */
@Serializable
data class Lesson(val id: String, val level: Int, val steps: List<Step>, val bookSteps: List<String> = emptyList())

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

/** Teach Tinku: Tinku says something wrong and the child corrects it aloud. */
@Serializable
@SerialName("teach_tinku")
data class TeachTinku(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val wrong: String,
    val answer: String,
    val accept: List<String> = emptyList(),
    val meaning: Meaning,
) : Step

/** One thing the child can say. [next] is the turn it leads to; null means the following turn. */
@Serializable
data class RoleplayReply(
    /** Shown and read as the model answer. {child} becomes the child's name. */
    val say: String,
    val meaning: Meaning,
    /** What counts as this reply. "*" stands for any words: "my name is *". */
    val patterns: List<String>,
    val next: String? = null,
)

@Serializable
data class RoleplayTurn(
    val id: String,
    /** What Tinku says. {child} becomes the child's name. */
    val tinku: String,
    val meaning: Meaning,
    val replies: List<RoleplayReply>,
)

/** A scripted spoken conversation with Tinku that branches on what the child says. */
@Serializable
@SerialName("roleplay")
data class Roleplay(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val scene: String,
    val turns: List<RoleplayTurn>,
) : Step

/**
 * Read along: Tinku reads the passage first, then the child reads it aloud and each word turns
 * green as it is heard. [sentences] may contain "" for a paragraph break. [words] are the key
 * words taught before reading, with their Hindi meanings. [lineMeanings], when given, has one entry
 * per line of [sentences] (blank for a paragraph break) so the child sees what each line means.
 */
@Serializable
@SerialName("read_along")
data class ReadAlong(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val title: L,
    val sentences: List<String>,
    val words: List<Word> = emptyList(),
    val lineMeanings: List<Meaning> = emptyList(),
) : Step {
    /** The words the child reads, in order, with their punctuation. */
    val readingWords: List<String> get() = sentences.filter { it.isNotBlank() }.flatMap { it.split(" ") }.filter { it.isNotBlank() }
}

/** A question about the passage just read, asked in the app language, answered in English. */
@Serializable
@SerialName("story_question")
data class StoryQuestion(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val question: L,
    val options: List<String>,
    val answer: String,
) : Step

/** Word Power: a new word with its Hindi, a simple English meaning and an example sentence. */
@Serializable
@SerialName("learn_word")
data class LearnWord(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val word: Word,
    val meaning: String,
    val example: String,
    /** The example sentence in Hindi, shown under the English. */
    val exampleMeaning: Meaning? = null,
) : Step

/** Spell the word you hear: letter tiles, or the keyboard when [typed] (Classes 5–8). */
@Serializable
@SerialName("spell_word")
data class SpellWord(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val word: Word,
    val typed: Boolean = false,
) : Step

// ---------- Exercises for school textbook chapters ----------

/**
 * A written question with choices: a textbook comprehension question, a riddle, a table cell.
 * [modelAnswer] is the full answer to write in the exam, shown and read out once answered.
 * [emojis], if given, is a picture for each option in the same order.
 */
@Serializable
@SerialName("choice")
data class Choice(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val question: String,
    val questionMeaning: Meaning? = null,
    val options: List<String>,
    val answer: String,
    val emojis: List<String> = emptyList(),
    val modelAnswer: String? = null,
    val modelMeaning: Meaning? = null,
) : Step

/** One word and its meaning; [meaning] is the Hindi shown once the pair is matched. */
@Serializable
data class MatchPair(val left: String, val right: String, val meaning: Meaning? = null)

/** Tap a word, then its meaning, until every pair is joined. */
@Serializable
@SerialName("match_pairs")
data class MatchPairs(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val instruction: L,
    val pairs: List<MatchPair>,
) : Step

/** One word of a crossword. [clue] holds ___ where the word goes. [given] words start filled in. */
@Serializable
data class CrosswordEntry(
    val number: Int,
    val answer: String,
    val row: Int,
    val col: Int,
    val across: Boolean,
    val clue: String,
    val given: Boolean = false,
)

/** Fill each clue's blank from a word bank; the word drops into the grid. */
@Serializable
@SerialName("crossword")
data class Crossword(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val rows: Int,
    val cols: Int,
    val entries: List<CrosswordEntry>,
) : Step

@Serializable
data class SortGroup(val id: String, val name: String, val emoji: String, val meaning: Meaning)

/** [groups] lists every group the item rightly belongs to (a crocodile is a water animal and a wild one). */
@Serializable
data class SortItem(val text: String, val emoji: String, val groups: List<String>)

/** Put each item into its group. */
@Serializable
@SerialName("sort_groups")
data class SortGroups(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val instruction: L,
    val groups: List<SortGroup>,
    val items: List<SortItem>,
) : Step

/** Build an English sentence from word tiles, guided by [prompt]. [also] are other right orders. */
@Serializable
@SerialName("build_sentence")
data class BuildSentence(
    override val id: String,
    override val skills: List<String> = emptyList(),
    val prompt: L,
    val answer: String,
    val meaning: Meaning,
    val extra: List<String> = emptyList(),
    val also: List<String> = emptyList(),
) : Step {
    val tiles: List<String> get() = answer.split(" ").filter { it.isNotBlank() }
}

/** Used as a reply's [RoleplayReply.next] to finish the conversation. */
const val END_CONVERSATION = "end"

const val BLANK = "___"

val FillBlank.filled: String get() = sentence.replace(BLANK, answer)

val SpotMistake.corrected: String
    get() = words.mapIndexed { i, w -> if (i == wrong) fix else w }.joinToString(" ")

val SentenceTrain.sentence: String get() = answer.joinToString(" ") { it.text }

val Step.isQuestion: Boolean get() = this !is LearnCard && this !is LearnSound && this !is LearnWord

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
        is TeachTinku -> answer
        is StoryQuestion -> answer
        is SpellWord -> word.english
        is Choice -> modelAnswer ?: answer
        is BuildSentence -> answer
        is LearnCard, is LearnSound, is Roleplay, is ReadAlong, is LearnWord, is MatchPairs, is Crossword, is SortGroups -> null
    }

/** Reading passages have their own ladder (R1.P01 …), separate from the lesson journey. */
val String.isReadingLesson: Boolean get() = startsWith("R") && contains(".P")
