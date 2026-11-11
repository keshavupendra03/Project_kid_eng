package com.samjhoenglish.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Measures time the app is on screen for the current child, for the parent dashboard and daily limit. */
class StudyTimer(private val progress: ProgressRepository, private val scope: CoroutineScope) {
    private var childId: Long? = null
    private var startedAt: Long? = null
    private var inParentArea = false

    /** App came to the foreground. */
    fun resume() {
        if (startedAt == null && !inParentArea) startedAt = System.currentTimeMillis()
    }

    /** App went to the background: save the time spent. */
    fun pause() {
        val start = startedAt ?: return
        startedAt = null
        val id = childId ?: return
        val end = System.currentTimeMillis()
        if (end - start >= MIN_MILLIS) scope.launch { progress.recordStudyTime(id, start, end) }
    }

    /** Saves the running time now, so today's total is up to date for the daily limit. */
    suspend fun flush() {
        val start = startedAt ?: return
        val id = childId ?: return
        val end = System.currentTimeMillis()
        if (end - start < MIN_MILLIS) return
        startedAt = end
        progress.recordStudyTime(id, start, end)
    }

    /** A parent looking at the dashboard isn't the child studying. */
    fun enterParentArea() {
        pause()
        inParentArea = true
    }

    fun leaveParentArea() {
        inParentArea = false
        resume()
    }

    fun setChild(id: Long) {
        if (id == childId) return
        val running = startedAt != null
        pause()
        childId = id
        if (running) resume()
    }

    /** The current child was deleted: don't record time against them any more. */
    fun clearChild() {
        childId = null
    }

    private companion object {
        const val MIN_MILLIS = 5_000L
    }
}
