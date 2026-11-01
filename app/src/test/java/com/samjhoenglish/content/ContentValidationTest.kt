package com.samjhoenglish.content

import com.samjhoenglish.i18n.L
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks every lesson file for mistakes a child would hit: answers missing from the options,
 * blank translations, duplicate ids, lessons the journey map points to but don't exist.
 * Run with: gradlew test
 */
class ContentValidationTest {
    private val root = File("src/main/assets/content")
    private val lessonFiles = File(root, "lessons").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

    @Test
    fun levelsPointToExistingLessons() {
        val levels = ContentJson.decodeFromString<LevelsFile>(File(root, "levels.json").readText()).levels
        val problems = mutableListOf<String>()
        levels.forEach { level ->
            level.title.check("level ${level.number} title", problems)
            level.lessons.forEach { ref ->
                ref.title.check("${ref.id} title", problems)
                if (!File(root, "lessons/${ref.id}.json").exists()) problems += "${ref.id}: listed in levels.json but file is missing"
            }
        }
        val listed = levels.flatMap { l -> l.lessons.map { it.id } }.toSet()
        lessonFiles.forEach { if (it.nameWithoutExtension !in listed) problems += "${it.name}: not listed in levels.json" }
        assertNoProblems(problems)
    }

    @Test
    fun everyLessonIsValid() {
        assertTrue("no lesson files found", lessonFiles.isNotEmpty())
        val problems = mutableListOf<String>()
        val allIds = mutableSetOf<String>()
        for (file in lessonFiles) {
            val lesson = runCatching { ContentJson.decodeFromString<Lesson>(file.readText()) }
                .onFailure { problems += "${file.name}: can't be read — ${it.message}" }
                .getOrNull() ?: continue
            if (lesson.id != file.nameWithoutExtension) problems += "${file.name}: id is ${lesson.id}"
            lesson.steps.forEach { step ->
                if (!allIds.add(step.id)) problems += "${step.id}: duplicate step id"
                if (!step.id.startsWith(lesson.id)) problems += "${step.id}: id should start with ${lesson.id}"
                checkStep(step, problems)
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun placementQuizIsValid() {
        val placement = ContentJson.decodeFromString<PlacementFile>(File(root, "placement.json").readText())
        val problems = mutableListOf<String>()
        val ids = mutableSetOf<String>()
        placement.sections.forEach { section ->
            if (section.steps.size < 3) problems += "placement level ${section.level}: needs at least 3 questions"
            section.steps.forEach { step ->
                if (!ids.add(step.id)) problems += "${step.id}: duplicate step id"
                if (!step.isQuestion) problems += "${step.id}: placement steps must be questions"
                // Speaking can't be judged fairly before the child has used the mic, so placement is tap-only.
                if (step is SayWord || step is SaySentence || step is Blend) problems += "${step.id}: no speaking in placement"
                checkStep(step, problems)
            }
        }
        assertNoProblems(problems)
    }

    private fun checkStep(step: Step, problems: MutableList<String>) {
        val id = step.id
        when (step) {
            is LearnCard -> {
                step.text.check(id, problems)
                // Words like "Monday" have no picture; the English word itself is required.
                step.word?.let { if (it.english.isBlank()) problems += "$id: word has no English" }
            }
            is ListenTap -> {
                val words = step.options.map { it.english }
                checkOptions(id, step.answer, words, problems)
                if (step.options.map { it.emoji }.toSet().size != step.options.size) problems += "$id: two options share a picture"
            }
            is WordPick -> checkOptions(id, step.answer, step.options, problems)
            is FindEnglish -> {
                if (step.words.none { it.en != null }) problems += "$id: no English word to find"
                if (step.words.all { it.en != null }) problems += "$id: every word is English, nothing to tell apart"
                step.words.forEach { if (it.hi.isBlank() || it.hinglish.isBlank()) problems += "$id: blank word" }
            }
            is LearnSound -> {
                step.text.check(id, problems)
                checkSound(id, step.sound, problems)
                if (step.hint.isBlank()) problems += "$id: missing Hindi hint"
            }
            is MinimalPair -> {
                checkOptions(id, step.answer, step.options, problems)
                if (step.options.size != 2) problems += "$id: a minimal pair needs exactly 2 words"
            }
            is SayWord -> if (step.word.english.isBlank()) problems += "$id: nothing to say"
            is SaySentence -> {
                if (step.sentence.isBlank()) problems += "$id: nothing to say"
                step.meaning.check(id, problems)
            }
            is Blend -> {
                step.parts.forEach { checkSound(id, it.sound, problems) }
                val spelled = step.parts.joinToString("") { it.letters }
                if (spelled != step.word) problems += "$id: parts spell '$spelled', not '${step.word}'"
            }
            is MatchMeaning -> {
                if (step.answer !in step.options) problems += "$id: answer meaning is not one of the options"
                if (step.options.toSet().size != step.options.size) problems += "$id: duplicate options"
                if (step.options.size !in 2..4) problems += "$id: needs 2-4 options"
                step.options.forEach { it.check(id, problems) }
            }
            is ListenRead -> checkOptions(id, step.answer, step.options, problems)
            is SentenceTrain -> {
                if (step.answer.size < 2) problems += "$id: a train needs at least 2 words"
                if (step.hindi.isEmpty()) problems += "$id: no Hindi sentence"
                (step.answer.map { it.role } + step.hindi.map { it.role }).forEach {
                    if (it !in ROLES) problems += "$id: unknown role '$it'"
                }
                step.hindi.forEach { Meaning(it.hi, it.hinglish).check(id, problems) }
                if (step.extra.any { extra -> step.answer.any { it.text == extra } }) problems += "$id: an extra word is also in the answer"
            }
            is FillBlank -> {
                if (step.sentence.split(BLANK).size != 2) problems += "$id: sentence needs exactly one $BLANK"
                checkOptions(id, step.answer, step.options, problems)
                step.meaning?.check(id, problems)
            }
            is SpotMistake -> {
                if (step.wrong !in step.words.indices) problems += "$id: wrong index out of range"
                else if (step.words[step.wrong] == step.fix) problems += "$id: the fix is the same as the wrong word"
                step.meaning?.check(id, problems)
            }
        }
    }

    private fun Meaning.check(where: String, problems: MutableList<String>) {
        if (hi.none { it in 'ऀ'..'ॿ' }) problems += "$where: Hindi '$hi' has no Devanagari"
        if (hinglish.isBlank() || hinglish.any { it in 'ऀ'..'ॿ' }) problems += "$where: bad Hinglish '$hinglish'"
    }

    /** The sound must have a file in assets/audio/phonics (an empty placeholder is fine until it's recorded). */
    private fun checkSound(id: String, sound: String, problems: MutableList<String>) {
        if (sound !in soundFiles) problems += "$id: no audio file for sound '$sound'"
    }

    private val soundFiles: Set<String> by lazy {
        File("src/main/assets/audio/phonics").listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
    }

    private fun checkOptions(id: String, answer: String, options: List<String>, problems: MutableList<String>) {
        if (answer !in options) problems += "$id: answer '$answer' is not one of the options"
        if (options.toSet().size != options.size) problems += "$id: duplicate options"
        if (options.size !in 2..4) problems += "$id: needs 2-4 options, has ${options.size}"
    }

    private fun L.check(where: String, problems: MutableList<String>) {
        if (hi.isBlank() || hinglish.isBlank() || en.isBlank()) problems += "$where: a language is blank"
        // Hindi text must be in Devanagari, Hinglish in Roman letters.
        if (hi.none { it in 'ऀ'..'ॿ' }) problems += "$where: hi text has no Devanagari"
        if (hinglish.any { it in 'ऀ'..'ॿ' }) problems += "$where: hinglish text contains Devanagari"
    }

    private companion object {
        val ROLES = setOf("s", "v", "o", "q", "x")
    }

    private fun assertNoProblems(problems: List<String>) {
        assertTrue("Content problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
