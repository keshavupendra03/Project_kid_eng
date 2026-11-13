package com.samjhoenglish.content

/**
 * School tests: which questions a teacher may put in a test, and how a question and its right
 * answer read in a report (the website, and the parent's result screen on the phone).
 *
 * Only tap-and-type questions go into tests, so every child can take them: no speaking, no
 * read-along, and no story questions (they only make sense right after their passage).
 */
object TestQuestions {
    data class Shown(val question: String, val answer: String)

    fun describe(step: Step): Shown? = when (step) {
        is Choice -> if (step.modelAnswer?.startsWith("Answer about yourself") == true) null else Shown(step.question, step.answer)
        is FillBlank -> Shown(step.sentence, step.answer)
        is SpotMistake -> Shown("Find the mistake: ${step.words.joinToString(" ")}", "${step.words.getOrElse(step.wrong) { "" }} → ${step.fix}")
        is BuildSentence -> Shown("Make the sentence: ${step.prompt.en}", step.answer)
        is SentenceTrain -> Shown("Make it in English: ${step.hindi.joinToString(" ") { it.hi }}", step.answer.joinToString(" ") { it.text })
        is MatchPairs -> Shown(step.instruction.en, step.pairs.joinToString("; ") { "${it.left} = ${it.right}" })
        is SortGroups -> {
            val names = step.groups.associate { it.id to it.name }
            Shown(step.instruction.en, step.items.joinToString("; ") { item -> "${item.text} → ${item.groups.joinToString(" / ") { names[it] ?: it }}" })
        }
        is Crossword -> Shown(
            "Crossword: ${step.entries.filterNot { it.given }.joinToString("; ") { it.clue }}",
            step.entries.filterNot { it.given }.joinToString(", ") { it.answer },
        )
        is WordPick -> Shown("${step.emoji} Pick the word for the picture", step.answer)
        is ListenTap -> Shown("Listen and tap the picture (hears “${step.answer}”)", step.answer)
        is ListenRead -> Shown("Listen and pick how it is written (hears “${step.answer}”)", step.answer)
        is MinimalPair -> Shown("Which word did you hear? ${step.options.joinToString(" / ")}", step.answer)
        is MatchMeaning -> Shown("Meaning of “${step.word.english}”", step.answer.hi)
        is SpellWord -> Shown("Spell the word you hear (“${step.word.english}”)", step.word.english)
        else -> null
    }

    fun canTest(step: Step): Boolean = describe(step) != null

    /**
     * Activities with several parts (match, sort, crossword): any slip makes the question wrong,
     * but the child finishes it. Every other question counts its first answer.
     */
    fun hasParts(step: Step): Boolean = step is MatchPairs || step is SortGroups || step is Crossword
}
