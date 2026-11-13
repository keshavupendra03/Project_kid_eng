package com.samjhoenglish.data

import com.samjhoenglish.content.ContentJson
import com.samjhoenglish.content.Lesson
import com.samjhoenglish.learning.SkillCatalog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Keeps the phone and the school website agreeing with each other. */
class SchoolContractTest {
    private val web = File("../web")

    @Test
    fun syncPayloadMatchesWhatTheWebsiteExpects() {
        val payload = SyncPayload(
            summary = SyncSummary(
                lessonsPassed = 12, stars = 30, wordsLearned = 45, sentencesSpoken = 6, streak = 3, level = 1, lessonId = "L1.U10",
                readingLevel = 2, passagesRead = 7, readingAccuracy = 86, wordsPerMinute = 41, expressBestWpm = 55,
                powerWordsLearned = 30, powerWordsMastered = 12, monthlyTestStars = 2,
            ),
            days = listOf(SyncDay(day = 0, minutes = 25)),
            skills = listOf(SyncSkill("sound.th", 0.25f, 6), SyncSkill("vocab.body", 0.9f, 8)),
            chapters = listOf(SyncChapter("B7.04", done = 3, total = 9, stars = 7, testStars = null), SyncChapter("B7.01", done = 9, total = 9, stars = 24, testStars = 3)),
        )
        val fixture = Json.parseToJsonElement(File(web, "test/fixtures/sync-payload.json").readText())
        assertEquals(fixture, Json.parseToJsonElement(Json.encodeToString(payload)))
    }

    @Test
    fun onlyTheAgreedFieldsCanBeSent() {
        val sent = Json.parseToJsonElement(File(web, "test/fixtures/sync-payload.json").readText()) as JsonObject
        assertEquals(setOf("summary", "days", "skills", "chapters"), sent.keys)
    }

    /** Run with UPDATE_WEB_SKILLS=1 after changing skill names, to refresh the website's copy. */
    @Test
    fun websiteHasEverySkillName() {
        val pretty = Json { prettyPrint = true }
        val root = File("src/main/assets/content/lessons")
        val fromLessons = root.listFiles().orEmpty().flatMap { f -> ContentJson.decodeFromString<Lesson>(f.readText()).steps.flatMap { it.skills } }
        val skills = (fromLessons + listOf("sound.th", "sound.v", "sound.w", "sound.z", "sound.f", "sound.sh", "sound.s_cluster")).toSortedSet()
        val expected = buildJsonObject {
            put("broad", buildJsonArray { SkillCatalog.broad.sorted().forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            putJsonObject("names") {
                skills.forEach { skill ->
                    val name = SkillCatalog.describe(skill)?.name ?: return@forEach
                    putJsonObject(skill) {
                        put("en", name.en)
                        put("hi", name.hi)
                    }
                }
            }
        }
        val file = File(web, "public/skills.json")
        if (System.getenv("UPDATE_WEB_SKILLS") == "1") file.writeText(pretty.encodeToString(JsonObject.serializer(), expected) + "\n")
        assertEquals("web/public/skills.json is out of date", expected, Json.parseToJsonElement(file.readText()))
    }
}
