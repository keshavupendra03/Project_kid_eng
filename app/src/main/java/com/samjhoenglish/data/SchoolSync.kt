package com.samjhoenglish.data

import com.samjhoenglish.BuildConfig
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.ListenRead
import com.samjhoenglish.content.ListenTap
import com.samjhoenglish.content.MatchMeaning
import com.samjhoenglish.content.SayWord
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.WordPick
import com.samjhoenglish.content.answerSpeech
import com.samjhoenglish.content.WORD_MONTHLY_PREFIX
import com.samjhoenglish.content.isWordPower
import com.samjhoenglish.content.lessonOrder
import com.samjhoenglish.data.db.ProgressDao
import com.samjhoenglish.data.db.SchoolLink
import com.samjhoenglish.data.db.SchoolTest
import com.samjhoenglish.data.db.TestAttempt
import com.samjhoenglish.content.Lesson
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.learning.WordPower
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Everything a school can ever receive from this phone. Voice recordings, other-app usage and
 * app locks have no place in these types, so they can't be sent by accident.
 */
@Serializable
data class SyncSummary(
    /** Lessons on the main path (not reading stories, Word Power or Tinku Express, which have their own numbers). */
    val lessonsPassed: Int,
    val stars: Int,
    val wordsLearned: Int,
    val sentencesSpoken: Int,
    val streak: Int,
    val level: Int?,
    val lessonId: String?,
    /** Reading ladder: level reached (R1 = 1), stories read, and how the last 10 readings went. */
    val readingLevel: Int?,
    val passagesRead: Int,
    val readingAccuracy: Int?,
    val wordsPerMinute: Int?,
    /** Fastest reading speed in Tinku Express. */
    val expressBestWpm: Int?,
    /** Word Power: words learned (10 per pack), words remembered again and again, latest monthly test stars. */
    val powerWordsLearned: Int,
    val powerWordsMastered: Int,
    val monthlyTestStars: Int?,
)

@Serializable
data class SyncDay(val day: Long, val minutes: Int)

@Serializable
data class SyncSkill(val skill: String, val mastery: Float, val attempts: Int)

/**
 * Progress in one chapter of the school textbook (e.g. B7.04): parts finished, stars, and the
 * chapter test's stars once taken. Sent only for chapters the child has started.
 */
@Serializable
data class SyncChapter(val chapter: String, val done: Int, val total: Int, val stars: Int, val testStars: Int?)

@Serializable
data class SyncPayload(val summary: SyncSummary, val days: List<SyncDay>, val skills: List<SyncSkill>, val chapters: List<SyncChapter>)

/** A test as the school sends it. [questions] are [lessonId, stepId] pairs, already in the app. */
@Serializable
data class ServerTest(
    val id: Long,
    val kind: String,
    val title: String,
    val minutes: Int,
    val closesAt: Long,
    val questions: List<List<String>>,
    val done: Boolean = false,
)

@Serializable
private data class ServerTests(val needsConsent: Boolean = false, val tests: List<ServerTest> = emptyList())

/** One answer in a handed-in test: the question, right or not, and what the child gave. */
@Serializable
data class TestAnswer(val l: String, val s: String, val ok: Boolean, val given: String = "")

@Serializable
private data class SentAnswer(val s: String, val ok: Boolean, val given: String)

@Serializable
private data class ResultBody(val answers: List<SentAnswer>, val seconds: Int)

@Serializable
private data class ConsentBody(val consent: Boolean, val consentVersion: String)

/** A new test for one child, for the "new test" notification. */
data class NewTest(val childId: Long, val studentName: String, val title: String)

@Serializable
private data class JoinRequest(val code: String, val consent: Boolean, val consentVersion: String)

@Serializable
private data class JoinResponse(val token: String, val studentName: String, val schoolName: String, val grade: Int, val section: String)

sealed interface LinkResult {
    data class Linked(val studentName: String, val schoolName: String) : LinkResult
    data object WrongCode : LinkResult
    data object TooManyTries : LinkResult
    data object NoConnection : LinkResult
}

/** Connects a child to their school and sends a small learning summary. Off unless a parent connects. */
class SchoolSync(
    private val dao: ProgressDao,
    private val content: ContentRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun link(childId: Long): Flow<SchoolLink?> = dao.schoolLink(childId)

    suspend fun connect(childId: Long, code: String): LinkResult {
        val body = json.encodeToString(JoinRequest(code, consent = true, consentVersion = CONSENT_VERSION))
        val (status, text) = post("/api/device/join", body, token = null) ?: return LinkResult.NoConnection
        return when (status) {
            201 -> {
                val r = json.decodeFromString<JoinResponse>(text)
                dao.upsertSchoolLink(SchoolLink(childId, r.token, r.schoolName, r.studentName, r.grade, r.section, linkedAt = System.currentTimeMillis()))
                sync(childId, force = true)
                LinkResult.Linked(r.studentName, r.schoolName)
            }
            429 -> LinkResult.TooManyTries
            404, 400 -> LinkResult.WrongCode
            else -> LinkResult.NoConnection
        }
    }

    /** Stops sharing. The server deletes what it holds. Returns false if the phone is offline. */
    suspend fun disconnect(childId: Long): Boolean {
        val link = dao.schoolLinkNow(childId) ?: return true
        val (status, _) = post("/api/device/unlink", "{}", link.deviceToken) ?: return false
        if (status != 200 && status != 401) return false
        dao.deleteSchoolLink(childId)
        dao.deleteSchoolTests(childId)
        return true
    }

    // ---------- School tests ----------

    /**
     * Asks the school which tests this child has, and keeps them on the phone so the child can take
     * them offline. Returns the tests not yet announced to the parent. Null if the phone is offline.
     */
    suspend fun fetchTests(childId: Long): List<NewTest>? {
        val link = dao.schoolLinkNow(childId) ?: return emptyList()
        val (status, text) = request("GET", "/api/device/tests", null, link.deviceToken) ?: return null
        if (status == 401) {
            dao.deleteSchoolLink(childId)
            dao.deleteSchoolTests(childId)
            return emptyList()
        }
        if (status != 200) return null
        val reply = json.decodeFromString<ServerTests>(text)
        if (reply.needsConsent != link.needsTestsConsent) dao.upsertSchoolLink(link.copy(needsTestsConsent = reply.needsConsent))
        val known = dao.schoolTestsNow(childId).associateBy { it.testId }
        val now = System.currentTimeMillis()
        val tests = reply.tests.filter { t -> t.questions.all { it.size == 2 } }.map { t ->
            SchoolTest(
                childId = childId, testId = t.id, kind = t.kind, title = t.title, minutes = t.minutes, closesAt = t.closesAt,
                questions = json.encodeToString(t.questions), receivedAt = known[t.id]?.receivedAt ?: now,
                notified = known[t.id]?.notified ?: false, doneOnServer = t.done,
            )
        }
        dao.upsertSchoolTests(tests)
        dao.deleteSchoolTestsExcept(childId, tests.map { it.testId }.ifEmpty { listOf(-1L) })
        val fresh = tests.filter { !it.notified && !it.doneOnServer && it.closesAt > now && dao.testAttempt(childId, it.testId)?.submittedAt == null }
        if (fresh.isNotEmpty()) dao.markTestsNotified(childId, fresh.map { it.testId })
        return fresh.map { NewTest(childId, link.studentName, it.title) }
    }

    /** Fetches tests for every connected child and hands in finished ones. Returns tests to announce. */
    suspend fun checkTests(): List<NewTest> {
        uploadResults()
        return dao.schoolLinks().flatMap { fetchTests(it.childId).orEmpty() }
    }

    /** Sends every finished test the school doesn't have yet. */
    suspend fun uploadResults() {
        for (attempt in dao.testsToUpload()) {
            val link = dao.schoolLinkNow(attempt.childId)
            if (link == null) {
                // Disconnected from school: the result stays on the phone for the parent only.
                dao.upsertTestAttempt(attempt.copy(uploaded = true))
                continue
            }
            val answers = json.decodeFromString<List<TestAnswer>>(attempt.answers)
            val body = json.encodeToString(ResultBody(answers.map { SentAnswer(it.s, it.ok, it.given.take(200)) }, attempt.seconds))
            val (status, _) = request("POST", "/api/device/tests/${attempt.testId}/result", body, link.deviceToken) ?: return
            // 404/409: the teacher deleted or closed the test; nothing more can be done with it.
            if (status == 200 || status == 404 || status == 409 || status == 400) dao.upsertTestAttempt(attempt.copy(uploaded = true))
        }
    }

    /** The parent agrees to the newer consent that covers tests. Returns false if offline. */
    suspend fun acceptTestsConsent(childId: Long): Boolean {
        val link = dao.schoolLinkNow(childId) ?: return false
        val (status, _) = request("POST", "/api/device/consent", json.encodeToString(ConsentBody(true, CONSENT_VERSION)), link.deviceToken) ?: return false
        if (status != 200) return false
        dao.upsertSchoolLink(link.copy(needsTestsConsent = false))
        fetchTests(childId)
        sync(childId, force = true)
        return true
    }

    /** The questions of a test, in order, or null if one is missing (the app needs updating). */
    suspend fun testSteps(test: SchoolTest): List<Pair<String, Step>>? {
        val pairs = json.decodeFromString<List<List<String>>>(test.questions)
        val lessons = HashMap<String, Lesson?>()
        return pairs.map { (lessonId, stepId) ->
            val lesson = lessons.getOrPut(lessonId) { runCatching { content.lesson(lessonId) }.getOrNull() } ?: return null
            val step = lesson.steps.firstOrNull { it.id == stepId } ?: return null
            lessonId to step
        }
    }

    fun activeTests(childId: Long): Flow<List<SchoolTest>> = dao.activeTests(childId, System.currentTimeMillis())

    fun results(childId: Long): Flow<List<TestAttempt>> = dao.testResults(childId)

    suspend fun test(childId: Long, testId: Long): SchoolTest? = dao.schoolTest(childId, testId)

    suspend fun attempt(childId: Long, testId: Long): TestAttempt? = dao.testAttempt(childId, testId)

    suspend fun saveAttempt(attempt: TestAttempt) = dao.upsertTestAttempt(attempt)

    fun answersOf(attempt: TestAttempt): List<TestAnswer> = json.decodeFromString(attempt.answers)

    /** The questions behind a handed-in test's answers, to show them again (questions no longer in the app are left out). */
    suspend fun stepsOf(answers: List<TestAnswer>): List<Pair<String, Step>> {
        val lessons = HashMap<String, Lesson?>()
        return answers.mapNotNull { a ->
            val lesson = lessons.getOrPut(a.l) { runCatching { content.lesson(a.l) }.getOrNull() } ?: return@mapNotNull null
            lesson.steps.firstOrNull { it.id == a.s }?.let { a.l to it }
        }
    }

    fun encodeAnswers(answers: List<TestAnswer>): String = json.encodeToString(answers)

    suspend fun syncAll() {
        dao.schoolLinks().forEach { sync(it.childId, force = false) }
    }

    /** Returns true if the school has the latest summary. */
    suspend fun sync(childId: Long, force: Boolean): Boolean {
        val link = dao.schoolLinkNow(childId) ?: return false
        val time = System.currentTimeMillis()
        if (!force && time - link.lastSyncAt < MIN_SYNC_GAP_MS) return true

        val payload = buildPayload(childId, link)
        val fingerprint = json.encodeToString(payload.copy(skills = emptyList()))
        if (!force && payload.skills.isEmpty() && fingerprint == link.lastSummary) return true

        val (status, _) = post("/api/device/sync", json.encodeToString(payload), link.deviceToken) ?: return false
        return when (status) {
            200 -> {
                dao.upsertSchoolLink(link.copy(lastSyncAt = time, skillsSentUntil = time, lastSummary = fingerprint))
                true
            }
            // The school issued a new code or removed the student: this phone is no longer connected.
            401 -> {
                dao.deleteSchoolLink(childId)
                false
            }
            else -> false
        }
    }

    private suspend fun buildPayload(childId: Long, link: SchoolLink): SyncPayload {
        val today = localDay(System.currentTimeMillis())
        val order = content.levels().lessonOrder
        val status = dao.lessonProgressNow(childId).associate {
            it.lessonId to Learning.LessonStatus(it.completed, it.bestStars, it.timesPlayed, it.skippedByPlacement)
        }
        val next = Learning.nextLesson(order, status)
        val index = content.stepIndex()
        val words = dao.correctStepsNow(childId)
            .mapNotNull { index[it]?.step }
            .filter(::isWordQuestion)
            .mapNotNull { it.answerSpeech?.lowercase() }
            .toSet()
        val mainPath = order.toSet()

        // Reading, worked out as on the parent dashboard.
        val readingLevels = content.readingLevels()
        val openPassages = Learning.unlockedPassages(readingLevels.map { level -> level.lessons.map { it.id } }, status)
        val readings = dao.readingResults(childId, READINGS_COUNTED).first()
        val wordsRead = readings.sumOf { it.wordsRead }
        val wordsTotal = readings.sumOf { it.wordsTotal }
        val seconds = readings.sumOf { it.seconds }

        return SyncPayload(
            summary = SyncSummary(
                lessonsPassed = status.filterKeys { it in mainPath }.values.count { Learning.isPassed(it) },
                stars = dao.lessonStarsNow(childId) + dao.approvedMissionCountNow(childId),
                wordsLearned = words.size,
                sentencesSpoken = dao.sentencesSpokenNow(childId),
                streak = Learning.streak(dao.practiceDaysNow(childId).toSet(), today),
                level = next?.let { id -> content.levels().firstOrNull { level -> level.lessons.any { it.id == id } }?.number },
                lessonId = next,
                readingLevel = readingLevels.lastOrNull { level -> level.lessons.firstOrNull()?.id in openPassages }?.number,
                passagesRead = dao.passagesRead(childId).first(),
                readingAccuracy = if (wordsTotal > 0) 100 * wordsRead / wordsTotal else null,
                wordsPerMinute = if (seconds > 0 && wordsRead > 0) 60 * wordsRead / seconds else null,
                expressBestWpm = dao.expressBestWpm(childId).first(),
                powerWordsLearned = status.count { (id, s) -> id.isWordPower && id.contains(".P") && !id.endsWith("T") && s.completed } * 10,
                powerWordsMastered = dao.wordsMastered(childId, WordPower.MASTERED_BOX).first(),
                monthlyTestStars = status.filterKeys { it.startsWith(WORD_MONTHLY_PREFIX) }
                    .maxByOrNull { it.key.removePrefix(WORD_MONTHLY_PREFIX).toIntOrNull() ?: 0 }?.value?.bestStars,
            ),
            days = Learning.lastDays(dao.dailyMillisNow(childId, today - DAYS_SENT + 1).associate { it.day to it.millis }, today, DAYS_SENT)
                .filter { (_, minutes) -> minutes > 0 }
                .map { (day, minutes) -> SyncDay(day, minutes) },
            skills = dao.skillsChangedSince(childId, link.skillsSentUntil).take(MAX_SKILLS).map { SyncSkill(it.skill, it.mastery, it.attempts) },
            chapters = chapterProgress(link.grade, status),
        )
    }

    /** The child's progress in each textbook chapter of their school class that they have started. */
    private suspend fun chapterProgress(grade: Int, status: Map<String, Learning.LessonStatus>): List<SyncChapter> {
        val chapters = content.book().classes.firstOrNull { it.grade == grade }?.chapters ?: return emptyList()
        return chapters.mapNotNull { chapter ->
            val parts = chapter.sections.filterNot { it.test }
            val done = parts.count { status[it.id]?.completed == true }
            val test = chapter.sections.firstOrNull { it.test }?.let { status[it.id] }?.takeIf { it.completed }
            if (done == 0 && test == null) return@mapNotNull null
            SyncChapter(
                chapter = chapter.id, done = done, total = parts.size,
                stars = parts.sumOf { status[it.id]?.bestStars ?: 0 }, testStars = test?.bestStars,
            )
        }
    }

    private fun isWordQuestion(step: Step): Boolean =
        (step is ListenTap || step is WordPick || step is MatchMeaning || step is ListenRead || step is SayWord) &&
            step.skills.any { it.startsWith("vocab.") }

    private suspend fun post(path: String, body: String, token: String?): Pair<Int, String>? = request("POST", path, body, token)

    /** Returns (status, body), or null if the server couldn't be reached. */
    private suspend fun request(method: String, path: String, body: String?, token: String?): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(BuildConfig.SCHOOL_API_URL + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                if (token != null) connection.setRequestProperty("authorization", "Bearer $token")
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("content-type", "application/json")
                    connection.outputStream.use { it.write(body.toByteArray()) }
                }
                val status = connection.responseCode
                val stream = if (status < 400) connection.inputStream else connection.errorStream
                status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            } finally {
                connection.disconnect()
            }
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        /** Bump when the words of the consent screen change. The server must accept the new version. */
        const val CONSENT_VERSION = "2026-11-v4"
        private const val MIN_SYNC_GAP_MS = 15 * 60_000L
        private const val READINGS_COUNTED = 10
        private const val DAYS_SENT = 7
        private const val MAX_SKILLS = 300
        private const val TIMEOUT_MS = 15_000
    }
}
