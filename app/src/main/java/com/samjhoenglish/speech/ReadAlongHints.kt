package com.samjhoenglish.speech

/**
 * What a listener is told to expect while a child reads a paragraph: each sentence, plus the
 * same sentence with one Indian-English slip ("we see tree birds"), so the slip can be heard
 * instead of being forced into the correct word.
 */
object ReadAlongHints {
    fun sentences(paragraph: String): List<List<String>> =
        paragraph.split(Regex("(?<=[.!?])\\s+"))
            .map { SpeechChecker.normalize(it).split(" ").filter { w -> w.isNotEmpty() } }
            .filter { it.isNotEmpty() }

    /** Each sentence followed by its one-slip versions. [knows] drops slip words a listener has no spelling for. */
    fun sentenceVersions(paragraph: String, knows: (String) -> Boolean = { true }): List<List<String>> =
        sentences(paragraph).flatMap { s ->
            listOf(s) + s.indices.flatMap { k ->
                SpeechChecker.slipVariants(s[k]).filter(knows).map { v -> s.toMutableList().also { it[k] = v } }
            }
        }.distinct()

    /** The slip words a listener may report for this paragraph, for showing on a test screen. */
    fun slipWords(paragraph: String, knows: (String) -> Boolean = { true }): List<String> =
        sentences(paragraph).flatten().distinct().flatMap { SpeechChecker.slipVariants(it).filter(knows) }.distinct()
}
