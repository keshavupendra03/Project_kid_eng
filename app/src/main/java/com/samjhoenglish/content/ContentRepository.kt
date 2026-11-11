package com.samjhoenglish.content

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A question and the lesson it belongs to. */
data class IndexedStep(val lessonId: String, val step: Step)

/** Loads lesson content bundled in assets/content. */
class ContentRepository(private val assets: AssetManager) {
    private var levelsCache: List<LevelInfo>? = null
    private var stepIndexCache: Map<String, IndexedStep>? = null

    suspend fun levels(): List<LevelInfo> = levelsCache ?: withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<LevelsFile>(read("content/levels.json")).levels
    }.also { levelsCache = it }

    /** The reading ladder: levels of passages, in the same shape as the lesson journey. */
    suspend fun readingLevels(): List<LevelInfo> = readingCache ?: withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<LevelsFile>(read("content/reading.json")).levels
    }.also { readingCache = it }

    private var readingCache: List<LevelInfo>? = null

    /** Tinku Express trains, slowest first. */
    suspend fun express(): List<ExpressLevel> = expressCache ?: withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<ExpressFile>(read("content/express.json")).levels
    }.also { expressCache = it }

    private var expressCache: List<ExpressLevel>? = null

    /** Word Power bands, youngest first. */
    suspend fun wordPower(): WordPowerFile = wordPowerCache ?: withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<WordPowerFile>(read("content/wordpower.json"))
    }.also { wordPowerCache = it }

    private var wordPowerCache: WordPowerFile? = null
    private var wordIndexCache: Map<String, IndexedStep>? = null

    /** Every step of every Word Power test by id, for reviews and the monthly test. */
    suspend fun wordTestIndex(): Map<String, IndexedStep> = wordIndexCache ?: wordPower().bands
        .flatMap { it.packs }
        .map { lesson(it.testId) }
        .flatMap { lesson -> lesson.steps.map { it.id to IndexedStep(lesson.id, it) } }
        .toMap()
        .also { wordIndexCache = it }

    suspend fun lesson(id: String): Lesson = withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<Lesson>(read("content/lessons/$id.json"))
    }

    suspend fun placement(): PlacementFile = withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<PlacementFile>(read("content/placement.json"))
    }

    suspend fun missions(): List<Mission> = withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<MissionsFile>(read("content/missions.json")).missions
    }

    /** Every step of every lesson by id, so review sessions can pull questions from anywhere. */
    suspend fun stepIndex(): Map<String, IndexedStep> = stepIndexCache ?: levels().lessonOrder
        .map { lesson(it) }
        .flatMap { lesson -> lesson.steps.map { it.id to IndexedStep(lesson.id, it) } }
        .toMap()
        .also { stepIndexCache = it }

    private fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
}
