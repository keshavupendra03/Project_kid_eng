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

    suspend fun lesson(id: String): Lesson = withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<Lesson>(read("content/lessons/$id.json"))
    }

    suspend fun placement(): PlacementFile = withContext(Dispatchers.IO) {
        ContentJson.decodeFromString<PlacementFile>(read("content/placement.json"))
    }

    /** Every step of every lesson by id, so review sessions can pull questions from anywhere. */
    suspend fun stepIndex(): Map<String, IndexedStep> = stepIndexCache ?: levels().lessonOrder
        .map { lesson(it) }
        .flatMap { lesson -> lesson.steps.map { it.id to IndexedStep(lesson.id, it) } }
        .toMap()
        .also { stepIndexCache = it }

    private fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
}
