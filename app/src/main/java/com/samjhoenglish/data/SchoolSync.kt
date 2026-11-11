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

@Serializable
data class SyncPayload(val summary: SyncSummary, val days: List<SyncDay>, val skills: List<SyncSkill>)

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
        return true
    }

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
        )
    }

    private fun isWordQuestion(step: Step): Boolean =
        (step is ListenTap || step is WordPick || step is MatchMeaning || step is ListenRead || step is SayWord) &&
            step.skills.any { it.startsWith("vocab.") }

    /** Returns (status, body), or null if the server couldn't be reached. */
    private suspend fun post(path: String, body: String, token: String?): Pair<Int, String>? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(BuildConfig.SCHOOL_API_URL + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.doOutput = true
                connection.setRequestProperty("content-type", "application/json")
                if (token != null) connection.setRequestProperty("authorization", "Bearer $token")
                connection.outputStream.use { it.write(body.toByteArray()) }
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
        const val CONSENT_VERSION = "2026-11-v3"
        private const val MIN_SYNC_GAP_MS = 15 * 60_000L
        private const val READINGS_COUNTED = 10
        private const val DAYS_SENT = 7
        private const val MAX_SKILLS = 300
        private const val TIMEOUT_MS = 15_000
    }
}
