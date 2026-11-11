package com.samjhoenglish.learning

import com.samjhoenglish.content.ContentJson
import com.samjhoenglish.content.FillBlank
import com.samjhoenglish.content.LearnWord
import com.samjhoenglish.content.Lesson
import com.samjhoenglish.content.SpellWord
import com.samjhoenglish.content.WordPowerFile
import com.samjhoenglish.content.isQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WordPowerTest {
    private val bands = listOf(3 to 4, 5 to 6, 7 to 8)

    @Test
    fun aChildStartsInTheBandForTheirClass() {
        assertEquals(0, WordPower.homeBand(1, bands))
        assertEquals(0, WordPower.homeBand(4, bands))
        assertEquals(1, WordPower.homeBand(5, bands))
        assertEquals(2, WordPower.homeBand(8, bands))
    }

    @Test
    fun aClassFiveChildDoesNotGetPetNamesButCanStillGoBack() {
        assertEquals(setOf(0, 1), WordPower.openBands(home = 1, testsPassed = listOf(0, 0, 0), placementRight = listOf(0, 0, 0)))
    }

    @Test
    fun aYoungerChildClimbsByPassingTestsOrByPlacement() {
        // Class 3 child: passes 8 tests of Class 3–4 → Class 5–6 opens; Class 7–8 still closed.
        assertEquals(setOf(0, 1), WordPower.openBands(home = 0, testsPassed = listOf(8, 0, 0), placementRight = listOf(0, 0, 0)))
        // Then 8 tests of Class 5–6 → Class 7–8 opens too.
        assertEquals(setOf(0, 1, 2), WordPower.openBands(home = 0, testsPassed = listOf(8, 8, 0), placementRight = listOf(0, 0, 0)))
        // A strong Class 3 child shows on the placement test that they know Class 7–8 words.
        assertEquals(setOf(0, 2), WordPower.openBands(home = 0, testsPassed = listOf(0, 0, 0), placementRight = listOf(6, 2, 5)))
    }

    @Test
    fun aPackTestPassesFromEightOutOfTen() {
        assertEquals(3, WordPower.testStars(9, 10))
        assertEquals(2, WordPower.testStars(8, 10))
        assertEquals(1, WordPower.testStars(7, 10))
        assertTrue(WordPower.testPassed(2))
    }

    @Test
    fun packsOpenOneAfterAnother() {
        assertTrue(WordPower.packOpen(0, emptyList()))
        assertEquals(false, WordPower.packOpen(1, listOf(false)))
        assertTrue(WordPower.packOpen(1, listOf(true)))
    }

    @Test
    fun theWordOfTheDayChangesDaily() {
        val words = listOf("curious", "brave", "honest")
        assertEquals("curious", WordPower.wordOfDay(words, 0))
        assertEquals("brave", WordPower.wordOfDay(words, 1))
        assertEquals(null, WordPower.wordOfDay(emptyList<String>(), 1))
    }

    /** 360 words, no word twice, every pack teaches all ten and tests all ten. */
    @Test
    fun wordPowerContentIsComplete() {
        val root = File("src/main/assets/content")
        val file = ContentJson.decodeFromString<WordPowerFile>(File(root, "wordpower.json").readText())
        val problems = mutableListOf<String>()
        val allWords = file.bands.flatMap { b -> b.packs.flatMap { p -> p.words.map { it.english } } }
        if (allWords.size != 360) problems += "expected 360 words, found ${allWords.size}"
        allWords.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { problems += "$it: in two packs" }
        file.bands.zipWithNext { a, b -> if (b.minClass <= a.maxClass) problems += "${b.id}: classes overlap ${a.id}" }
        file.bands.forEach { band ->
            band.packs.forEach { pack ->
                val learn = ContentJson.decodeFromString<Lesson>(File(root, "lessons/${pack.id}.json").readText())
                val test = ContentJson.decodeFromString<Lesson>(File(root, "lessons/${pack.testId}.json").readText())
                val taught = learn.steps.filterIsInstance<LearnWord>().map { it.word.english }.toSet()
                if (taught != pack.words.map { it.english }.toSet()) problems += "${pack.id}: doesn't teach every word"
                if (test.steps.count { it.isQuestion } != 10) problems += "${pack.testId}: needs 10 questions"
                if (band.id != "W34" && test.steps.filterIsInstance<SpellWord>().any { !it.typed }) problems += "${pack.testId}: Class 5–8 spell by typing"
                if (band.id == "W34" && test.steps.filterIsInstance<SpellWord>().any { it.typed }) problems += "${pack.testId}: Class 3–4 spell with tiles"
                test.steps.filterIsInstance<FillBlank>().forEach { if (it.answer !in it.options) problems += "${it.id}: answer missing" }
                pack.words.forEach { w ->
                    if (w.hindi.none { it in 'ऀ'..'ॿ' }) problems += "${w.english}: Hindi has no Devanagari"
                    if (w.hinglish.any { it in 'ऀ'..'ॿ' }) problems += "${w.english}: Hinglish has Devanagari"
                    if (!Regex("\\b${Regex.escape(w.english)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(w.example)) problems += "${w.english}: not in its example"
                }
            }
        }
        val placement = ContentJson.decodeFromString<Lesson>(File(root, "lessons/${file.placement}.json").readText())
        file.bands.forEach { band ->
            val n = placement.steps.count { it.id.contains(".${band.id}.") }
            if (n < WordPower.PLACEMENT_TO_OPEN) problems += "placement: only $n questions for ${band.id}"
        }
        assertTrue("Word Power problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
