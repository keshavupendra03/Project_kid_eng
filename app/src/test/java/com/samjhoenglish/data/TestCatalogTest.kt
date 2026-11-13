package com.samjhoenglish.data

import com.samjhoenglish.content.BookFile
import com.samjhoenglish.content.ContentJson
import com.samjhoenglish.content.Lesson
import com.samjhoenglish.content.LevelsFile
import com.samjhoenglish.content.TestQuestions
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The website's copy of every question a teacher can put in a test: web/public/catalog/.
 * book-6/7/8.json hold the textbook's own questions by chapter; course.json the app's main course
 * by level and lesson. The phone plays the questions; the website only shows their text.
 *
 * Run with UPDATE_WEB_SKILLS=1 after changing content, to refresh the website's copy.
 */
class TestCatalogTest {
    private val content = File("src/main/assets/content")
    private val out = File("../web/public/catalog")
    private val lessons = HashMap<String, Lesson>()

    private fun lesson(id: String): Lesson = lessons.getOrPut(id) {
        ContentJson.decodeFromString<Lesson>(File(content, "lessons/$id.json").readText())
    }

    /** Questions of [lesson] a test may use, as {l, s, q, a}; [bookOnly] keeps the textbook's own. */
    private fun kotlinx.serialization.json.JsonArrayBuilder.questions(lesson: Lesson, bookOnly: Boolean, seen: MutableSet<String>) {
        for (step in lesson.steps) {
            if (bookOnly && step.id !in lesson.bookSteps) continue
            val shown = TestQuestions.describe(step) ?: continue
            // The same question can appear in a section and again in a chapter test: list it once.
            if (!seen.add("${shown.question}|${shown.answer}")) continue
            addJsonObject {
                put("l", lesson.id)
                put("s", step.id)
                put("q", shown.question)
                put("a", shown.answer)
            }
        }
    }

    private fun expected(): Map<String, JsonObject> {
        val files = LinkedHashMap<String, JsonObject>()
        val book = ContentJson.decodeFromString<BookFile>(File(content, "book.json").readText())
        for (c in book.classes) {
            files["book-${c.grade}.json"] = buildJsonObject {
                put("grade", c.grade)
                putJsonArray("chapters") {
                    for (chapter in c.chapters) addJsonObject {
                        put("id", chapter.id)
                        put("number", chapter.number)
                        put("emoji", chapter.emoji)
                        put("title", chapter.title)
                        put("pages", chapter.pages)
                        put("sections", chapter.sections.count { !it.test })
                        val seen = HashSet<String>()
                        putJsonArray("questions") {
                            // The chapter test only repeats section questions, so sections are enough.
                            chapter.sections.filterNot { it.test }.forEach { questions(lesson(it.id), bookOnly = true, seen) }
                        }
                    }
                }
            }
        }
        val levels = ContentJson.decodeFromString<LevelsFile>(File(content, "levels.json").readText())
        files["course.json"] = buildJsonObject {
            putJsonArray("levels") {
                for (level in levels.levels) addJsonObject {
                    put("number", level.number)
                    putJsonObject("title") { put("en", level.title.en); put("hi", level.title.hi) }
                    putJsonArray("lessons") {
                        for (ref in level.lessons) addJsonObject {
                            put("id", ref.id)
                            put("emoji", ref.emoji)
                            putJsonObject("title") { put("en", ref.title.en); put("hi", ref.title.hi) }
                            putJsonArray("questions") { questions(lesson(ref.id), bookOnly = false, HashSet()) }
                        }
                    }
                }
            }
        }
        return files
    }

    @Test
    fun websiteHasEveryTestQuestion() {
        val compact = Json { prettyPrint = false }
        val expected = expected()
        if (System.getenv("UPDATE_WEB_SKILLS") == "1") {
            out.mkdirs()
            expected.forEach { (name, body) -> File(out, name).writeText(compact.encodeToString(JsonObject.serializer(), body) + "\n") }
        }
        expected.forEach { (name, body) ->
            val file = File(out, name)
            assertTrue("web/public/catalog/$name is missing: run the tests with UPDATE_WEB_SKILLS=1", file.exists())
            assertEquals("web/public/catalog/$name is out of date: run the tests with UPDATE_WEB_SKILLS=1", body, Json.parseToJsonElement(file.readText()))
        }
    }

    @Test
    fun everyChapterAndCourseLessonHasQuestionsForATest() {
        val problems = mutableListOf<String>()
        for ((name, body) in expected()) {
            val groups: List<JsonElement> = if (name == "course.json") {
                (body["levels"] as JsonArray).flatMap { (it as JsonObject)["lessons"] as JsonArray }
            } else {
                body["chapters"] as JsonArray
            }
            for (g in groups) {
                g as JsonObject
                if ((g["questions"] as JsonArray).isEmpty() && name != "course.json") problems += "$name ${g["id"]}: no textbook questions a test can use"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
