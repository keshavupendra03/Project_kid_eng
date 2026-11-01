package com.samjhoenglish.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Measures time the app is on screen for the current child, for the parent dashboard and daily limit. */
class StudyTimer(private val progress: ProgressRepository, private val scope: CoroutineScope) {
    private var childId: Long? = null
    private var startedAt: Long? = null

    /** App came to the foreground. */
    fun resume() {
        if (startedAt == null) startedAt = System.currentTimeMillis()
    }

    /** App went to the background: save the time spent. */
    fun pause() {
        val start = startedAt ?: return
        startedAt = null
        val id = childId ?: return
        val end = System.currentTimeMillis()
        if (end - start >= MIN_MILLIS) scope.launch { progress.recordStudyTime(id, start, end) }
    }

    fun setChild(id: Long) {
        if (id == childId) return
        val running = startedAt != null
        pause()
        childId = id
        if (running) resume()
    }

    private companion object {
        const val MIN_MILLIS = 5_000L
    }
}
