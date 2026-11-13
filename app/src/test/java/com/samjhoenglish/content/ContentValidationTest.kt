package com.samjhoenglish.content

import com.samjhoenglish.i18n.L
import com.samjhoenglish.learning.SkillCatalog
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
        val reading = ContentJson.decodeFromString<LevelsFile>(File(root, "reading.json").readText()).levels
        val problems = mutableListOf<String>()
        (levels + reading).forEach { level ->
            level.title.check("level ${level.number} title", problems)
            level.lessons.forEach { ref ->
                ref.title.check("${ref.id} title", problems)
                if (!File(root, "lessons/${ref.id}.json").exists()) problems += "${ref.id}: listed but file is missing"
            }
        }
        reading.forEach { level -> level.lessons.forEach { if (!it.id.isReadingLesson) problems += "${it.id}: reading passages must be named R<level>.P<n>" } }
        levels.forEach { level -> level.lessons.forEach { if (it.id.isReadingLesson) problems += "${it.id}: a reading passage is in the lesson journey" } }
        val words = ContentJson.decodeFromString<WordPowerFile>(File(root, "wordpower.json").readText())
        val wordLessons = words.bands.flatMap { b -> b.packs.flatMap { listOf(it.id, it.testId) } } + words.placement
        wordLessons.forEach { if (!File(root, "lessons/$it.json").exists()) problems += "$it: listed in wordpower.json but file is missing" }
        val book = ContentJson.decodeFromString<BookFile>(File(root, "book.json").readText())
        val bookLessons = book.classes.flatMap { c -> c.chapters.flatMap { ch -> ch.sections.map { it.id } } }
        bookLessons.forEach {
            if (!File(root, "lessons/$it.json").exists()) problems += "$it: listed in book.json but file is missing"
            if (!it.isBookLesson) problems += "$it: textbook sections must be named B<class>.<chapter>.<section>"
        }
        val listed = (levels + reading).flatMap { l -> l.lessons.map { it.id } }.toSet() + wordLessons + bookLessons
        lessonFiles.forEach { if (it.nameWithoutExtension !in listed) problems += "${it.name}: not listed in levels.json, reading.json, wordpower.json or book.json" }
        assertNoProblems(problems)
    }

    /** Textbook chapters: sections in order, book questions that exist, crosswords whose words really cross. */
    @Test
    fun bookChaptersAreWellFormed() {
        val book = ContentJson.decodeFromString<BookFile>(File(root, "book.json").readText())
        val problems = mutableListOf<String>()
        book.classes.forEach { cls ->
            if (cls.grade !in 6..8) problems += "class ${cls.grade}: the textbook chapters are for Classes 6-8"
            cls.chapters.forEach { chapter ->
                if (chapter.id != "B${cls.grade}.%02d".format(chapter.number)) problems += "${chapter.id}: id doesn't match class ${cls.grade}, lesson ${chapter.number}"
                if (chapter.sections.count { it.test } != 1 || !chapter.sections.last().test) problems += "${chapter.id}: needs exactly one test, at the end"
                chapter.sections.forEach { section ->
                    section.title.check("${section.id} title", problems)
                    if (section.id.bookChapterId != chapter.id) problems += "${section.id}: not in chapter ${chapter.id}"
                    val file = File(root, "lessons/${section.id}.json")
                    if (!file.exists()) return@forEach
                    val lesson = ContentJson.decodeFromString<Lesson>(file.readText())
                    if (lesson.level != cls.grade) problems += "${section.id}: level should be the class, ${cls.grade}"
                    val ids = lesson.steps.map { it.id }.toSet()
                    lesson.bookSteps.forEach { if (it !in ids) problems += "${section.id}: book step $it doesn't exist" }
                    lesson.bookSteps.forEach { id -> if (lesson.steps.first { it.id == id }.let { !it.isQuestion }) problems += "$id: only questions can be marked as book questions" }
                    if (section.book && lesson.bookSteps.isEmpty()) problems += "${section.id}: a book section has no book questions"
                    if (lesson.steps.none { it.isQuestion }) problems += "${section.id}: no questions"
                    lesson.steps.filterIsInstance<Crossword>().forEach { checkCrossword(it, problems) }
                }
            }
        }
        assertNoProblems(problems)
    }

    private fun checkCrossword(step: Crossword, problems: MutableList<String>) {
        val grid = mutableMapOf<Pair<Int, Int>, Char>()
        step.entries.forEach { entry ->
            if (step.entries.count { it.number == entry.number } > 1) problems += "${step.id}: number ${entry.number} used twice"
            if (entry.clue.split(BLANK).size != 2) problems += "${step.id}/${entry.number}: clue needs exactly one $BLANK"
            if (!entry.answer.all { it.isLetter() }) problems += "${step.id}/${entry.number}: only letters fit in the grid"
            entry.cells.forEachIndexed { i, cell ->
                if (cell.first !in 0 until step.rows || cell.second !in 0 until step.cols) problems += "${step.id}/${entry.number}: runs off the grid"
                val letter = entry.answer[i].lowercaseChar()
                val before = grid.put(cell, letter)
                if (before != null && before != letter) problems += "${step.id}/${entry.number}: '$letter' crosses '$before' at $cell"
            }
        }
        if (step.entries.none { !it.given }) problems += "${step.id}: nothing to solve"
    }

    /**
     * A picture question must have exactly one right answer: no "kind of" pairs (bird and parrot),
     * no words a picture can't show (this, that, look), and no option that the picture could also mean
     * (🐘 with "elephant" and "big"). Rules live in tools/content/kinds.json, shared with the generator.
     */
    @Test
    fun pictureQuestionsHaveOneRightAnswer() {
        val rules = kotlinx.serialization.json.Json.parseToJsonElement(File("../tools/content/kinds.json").readText()) as kotlinx.serialization.json.JsonObject
        val kinds = (rules["kinds"] as kotlinx.serialization.json.JsonObject).mapValues { (_, v) ->
            (v as kotlinx.serialization.json.JsonArray).map { (it as kotlinx.serialization.json.JsonPrimitive).content.lowercase() }.toSet()
        }
        val noPicture = (rules["noPicture"] as kotlinx.serialization.json.JsonArray).map { (it as kotlinx.serialization.json.JsonPrimitive).content.lowercase() }.toSet()
        fun related(a: String, b: String) = a != b && (b in kinds[a].orEmpty() || a in kinds[b].orEmpty())

        val lessons = lessonFiles.map { ContentJson.decodeFromString<Lesson>(it.readText()) }
        // Which words each picture stands for, anywhere in the app.
        val pictureWords = mutableMapOf<String, MutableSet<String>>()
        fun note(w: Word) { if (w.emoji.isNotEmpty()) pictureWords.getOrPut(w.emoji) { mutableSetOf() } += w.english.lowercase() }
        lessons.flatMap { it.steps }.forEach { step ->
            when (step) {
                is LearnCard -> step.word?.let(::note)
                is ListenTap -> step.options.forEach(::note)
                is WordPick -> note(Word(step.answer, step.emoji))
                is SayWord -> note(step.word)
                is MatchMeaning -> note(step.word)
                is LearnWord -> note(step.word)
                else -> Unit
            }
        }

        val problems = mutableListOf<String>()
        lessons.flatMap { it.steps }.forEach { step ->
            when (step) {
                is WordPick -> {
                    val answer = step.answer.lowercase()
                    if (answer in noPicture) problems += "${step.id}: no picture can show '$answer'; ask it by sound"
                    step.options.map { it.lowercase() }.filter { it != answer }.forEach { other ->
                        if (related(answer, other)) problems += "${step.id}: '$answer' and '$other' are both right (one is a kind of the other)"
                        if (other in pictureWords[step.emoji].orEmpty()) problems += "${step.id}: the picture ${step.emoji} also means '$other'"
                    }
                }
                is ListenTap -> {
                    val answer = step.answer.lowercase()
                    if (answer in noPicture) problems += "${step.id}: no picture can show '$answer'; ask it by sound"
                    val pictures = step.options.map { it.emoji }
                    if (pictures.toSet().size != pictures.size) problems += "${step.id}: two options share a picture"
                    step.options.map { it.english.lowercase() }.filter { it != answer }.forEach { other ->
                        if (related(answer, other)) problems += "${step.id}: '$answer' and '$other' are both right (one is a kind of the other)"
                    }
                }
                else -> Unit
            }
        }
        assertNoProblems(problems)
    }

    /** Every passage teaches its key words first, then reads, then asks about what was read. */
    @Test
    fun readingPassagesAreWellFormed() {
        val problems = mutableListOf<String>()
        val passages = lessonFiles.filter { it.nameWithoutExtension.isReadingLesson }
        assertTrue("no reading passages", passages.size >= 30)
        passages.forEach { file ->
            val lesson = ContentJson.decodeFromString<Lesson>(file.readText())
            val read = lesson.steps.filterIsInstance<ReadAlong>()
            if (read.size != 1) problems += "${lesson.id}: needs exactly one read_along step"
            val passage = read.firstOrNull() ?: return@forEach
            val text = passage.sentences.joinToString(" ").lowercase()
            if (lesson.steps.count { it is StoryQuestion } < 2) problems += "${lesson.id}: needs 2 story questions"
            if (lesson.steps.indexOfFirst { it is StoryQuestion } < lesson.steps.indexOf(passage)) problems += "${lesson.id}: a question comes before the reading"
            passage.words.forEach { if (it.english.lowercase() !in text) problems += "${lesson.id}: key word '${it.english}' is not in the passage" }
            if (Regex("\\d").containsMatchIn(text)) problems += "${lesson.id}: digits in a passage (the listener hears numbers as words)"
            if (text.contains("mr.") || text.contains("mrs.")) problems += "${lesson.id}: abbreviation with a full stop breaks sentence splitting"
            val words = passage.readingWords.size
            val level = lesson.id.substring(1, 2).toInt()
            val limit = mapOf(1 to 25, 2 to 35, 3 to 60, 4 to 90, 5 to 110)
            if (words > limit.getValue(level)) problems += "${lesson.id}: $words words is too long for reading level $level"
        }
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
    fun everySkillHasAParentFriendlyName() {
        val problems = mutableListOf<String>()
        val skills = lessonFiles.flatMap { f -> ContentJson.decodeFromString<Lesson>(f.readText()).steps.flatMap { it.skills } }.toSet()
        (skills + "sound.th" + "sound.v" + "sound.w" + "sound.z" + "sound.f" + "sound.sh" + "sound.s_cluster").forEach { skill ->
            val info = SkillCatalog.describe(skill)
            if (info == null) problems += "$skill: no name for the parent section" else {
                info.name.check("$skill name", problems, needsDevanagari = false)
                info.tip.check("$skill tip", problems, needsDevanagari = false)
            }
        }
        assertNoProblems(problems)
    }

    @Test
    fun missionsAreValidAndInOrder() {
        val missions = ContentJson.decodeFromString<MissionsFile>(File(root, "missions.json").readText()).missions
        val lessonCount = lessonFiles.size
        val problems = mutableListOf<String>()
        if (missions.map { it.id }.toSet().size != missions.size) problems += "duplicate mission id"
        missions.zipWithNext { a, b -> if (b.needLessons < a.needLessons) problems += "${b.id}: out of order" }
        missions.forEach {
            it.text.check(it.id, problems)
            if (it.needLessons !in 0..lessonCount) problems += "${it.id}: needs ${it.needLessons} lessons, only $lessonCount exist"
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
            is TeachTinku -> {
                if (step.wrong == step.answer) problems += "$id: wrong and answer are the same"
                step.meaning.check(id, problems)
            }
            is Roleplay -> checkRoleplay(step, problems)
            is ReadAlong -> {
                step.title.check(id, problems)
                if (step.sentences.none { it.isNotBlank() }) problems += "$id: nothing to read"
                if (step.sentences.firstOrNull().isNullOrBlank() || step.sentences.lastOrNull().isNullOrBlank()) problems += "$id: passage starts or ends with a blank line"
                step.words.forEach { if (it.english.isBlank() || it.hindi.isNullOrBlank()) problems += "$id: key word needs English and Hindi" }
                // Book poems must say what every line means; elsewhere it is optional but must line up.
                if (id.startsWith("B") && step.lineMeanings.isEmpty()) problems += "$id: book poem needs a meaning for each line"
                if (step.lineMeanings.isNotEmpty()) {
                    if (step.lineMeanings.size != step.sentences.size) {
                        problems += "$id: ${step.lineMeanings.size} line meanings for ${step.sentences.size} lines"
                    } else {
                        step.sentences.zip(step.lineMeanings).forEachIndexed { n, (line, m) ->
                            if (line.isBlank() != (m.hi.isBlank() && m.hinglish.isBlank())) problems += "$id: line ${n + 1} meaning doesn't match a line"
                            else if (line.isNotBlank()) m.check("$id line ${n + 1}", problems)
                        }
                    }
                }
            }
            is StoryQuestion -> {
                step.question.check(id, problems)
                checkOptions(id, step.answer, step.options, problems)
            }
            is LearnWord -> {
                if (step.word.english.isBlank() || step.word.hindi.isNullOrBlank()) problems += "$id: word needs English and Hindi"
                if (step.meaning.isBlank() || step.example.isBlank()) problems += "$id: needs a meaning and an example"
                step.exampleMeaning?.check(id, problems) ?: run { problems += "$id: the example has no Hindi meaning" }
            }
            is SpellWord -> if (!step.word.english.all { it.isLetter() }) problems += "$id: only letters can be spelled with tiles or typing"
            is Choice -> {
                if (step.question.isBlank()) problems += "$id: no question"
                checkOptions(id, step.answer, step.options, problems)
                if (step.emojis.isNotEmpty() && step.emojis.size != step.options.size) problems += "$id: needs one picture per option, or none"
                step.questionMeaning?.check(id, problems)
                if ((step.modelAnswer == null) != (step.modelMeaning == null)) problems += "$id: an exam answer needs its Hindi meaning"
                step.modelMeaning?.check(id, problems)
            }
            is MatchPairs -> {
                step.instruction.check(id, problems)
                if (step.pairs.size !in 3..8) problems += "$id: needs 3-8 pairs"
                if (step.pairs.map { it.left }.toSet().size != step.pairs.size) problems += "$id: a word appears twice"
                if (step.pairs.map { it.right }.toSet().size != step.pairs.size) problems += "$id: a meaning appears twice"
                step.pairs.forEach { it.meaning?.check(id, problems) }
            }
            is Crossword -> Unit // Checked in bookChaptersAreWellFormed.
            is SortGroups -> {
                step.instruction.check(id, problems)
                val groups = step.groups.map { it.id }.toSet()
                if (groups.size != step.groups.size) problems += "$id: duplicate group"
                step.groups.forEach { it.meaning.check(id, problems) }
                step.items.forEach { item ->
                    if (item.groups.isEmpty() || item.groups.any { it !in groups }) problems += "$id: '${item.text}' points to a missing group"
                }
                if (step.items.map { it.text }.toSet().size != step.items.size) problems += "$id: an item appears twice"
            }
            is BuildSentence -> {
                step.prompt.check(id, problems)
                step.meaning.check(id, problems)
                if (step.tiles.size < 2) problems += "$id: a sentence needs at least 2 words"
                if (step.extra.any { it in step.tiles }) problems += "$id: an extra tile is also in the answer"
                // The English prompt must not give the answer away.
                if (step.prompt.en.contains(step.answer)) problems += "$id: the English prompt contains the answer"
            }
        }
    }

    /** Every turn must be reachable, every reply must lead somewhere, and every path must reach the end. */
    private fun checkRoleplay(step: Roleplay, problems: MutableList<String>) {
        val id = step.id
        val order = step.turns.map { it.id }
        if (order.toSet().size != order.size) problems += "$id: duplicate turn id"
        val byId = step.turns.associateBy { it.id }

        fun nextOf(turnIndex: Int, reply: RoleplayReply) = reply.next ?: order.getOrNull(turnIndex + 1) ?: END_CONVERSATION

        step.turns.forEachIndexed { index, turn ->
            turn.meaning.check("$id/${turn.id}", problems)
            if (turn.replies.isEmpty()) problems += "$id/${turn.id}: no replies"
            turn.replies.forEach { reply ->
                reply.meaning.check("$id/${turn.id}", problems)
                if (reply.patterns.isEmpty()) problems += "$id/${turn.id}: reply has no patterns"
                val next = nextOf(index, reply)
                if (next != END_CONVERSATION && next !in byId) problems += "$id/${turn.id}: goes to unknown turn '$next'"
            }
        }

        val reached = mutableSetOf<String>()
        var reachesEnd = false
        fun walk(turnId: String, path: Set<String>) {
            if (turnId == END_CONVERSATION) {
                reachesEnd = true
                return
            }
            if (turnId in path) {
                problems += "$id: the conversation can loop forever at '$turnId'"
                return
            }
            reached += turnId
            val turn = byId[turnId] ?: return
            turn.replies.forEach { walk(nextOf(order.indexOf(turnId), it), path + turnId) }
        }
        walk(order.first(), emptySet())
        if (!reachesEnd) problems += "$id: the conversation never ends"
        (order - reached).forEach { problems += "$id: turn '$it' can never be reached" }
    }

    private fun Meaning.check(where: String, problems: MutableList<String>) {
        if (hi.none { it in 'ऀ'..'ॿ' }) problems += "$where: Hindi '$hi' has no Devanagari"
        if (hinglish.isBlank() || hinglish.any { it in 'ऀ'..'ॿ' }) problems += "$where: bad Hinglish '$hinglish'"
    }

    /** The sound must have a file in assets/audio/phonics (an empty placeholder is fine until it's recorded). */
    private fun checkSound(id: String, sound: String, problems: MutableList<String>) {
        sound.split("+").forEach { if (it !in soundFiles) problems += "$id: no audio file for sound '$it'" }
    }

    private val soundFiles: Set<String> by lazy {
        File("src/main/assets/audio/phonics").listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
    }

    private fun checkOptions(id: String, answer: String, options: List<String>, problems: MutableList<String>) {
        if (answer !in options) problems += "$id: answer '$answer' is not one of the options"
        if (options.toSet().size != options.size) problems += "$id: duplicate options"
        if (options.size !in 2..4) problems += "$id: needs 2-4 options, has ${options.size}"
    }

    private fun L.check(where: String, problems: MutableList<String>, needsDevanagari: Boolean = true) {
        if (hi.isBlank() || hinglish.isBlank() || en.isBlank()) problems += "$where: a language is blank"
        // Hindi text must be in Devanagari, Hinglish in Roman letters. (Names like "I am" stay in English.)
        if (needsDevanagari && hi.none { it in 'ऀ'..'ॿ' }) problems += "$where: hi text has no Devanagari"
        if (hinglish.any { it in 'ऀ'..'ॿ' }) problems += "$where: hinglish text contains Devanagari"
    }

    private companion object {
        val ROLES = setOf("s", "v", "o", "q", "x")
    }

    private fun assertNoProblems(problems: List<String>) {
        assertTrue("Content problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
