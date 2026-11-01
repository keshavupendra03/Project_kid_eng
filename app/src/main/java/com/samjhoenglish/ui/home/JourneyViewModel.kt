package com.samjhoenglish.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.LessonRef
import com.samjhoenglish.content.LevelInfo
import com.samjhoenglish.content.lessonOrder
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.learning.Learning
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** A child's journey: which lessons are open, passed, how many stars, and what to do next. */
data class JourneyUiState(
    val loaded: Boolean = false,
    val levels: List<LevelInfo> = emptyList(),
    val unlocked: Set<String> = emptySet(),
    val passed: Set<String> = emptySet(),
    val stars: Map<String, Int> = emptyMap(),
    val nextLesson: LessonRef? = null,
    val dueCount: Int = 0,
    val streak: Int = 0,
) {
    val totalStars: Int get() = stars.values.sum()

    fun isOpen(level: LevelInfo): Boolean = level.lessons.firstOrNull()?.id in unlocked

    fun passedCount(level: LevelInfo): Int = level.lessons.count { it.id in passed }
}

class JourneyViewModel(
    childId: Long,
    content: ContentRepository,
    progress: ProgressRepository,
) : ViewModel() {
    val uiState: StateFlow<JourneyUiState> = combine(
        flow { emit(content.levels()) },
        progress.lessonStatus(childId),
        progress.dueCount(childId),
        progress.streak(childId),
    ) { levels, status, due, streak ->
        val order = levels.lessonOrder
        val nextId = Learning.nextLesson(order, status)
        JourneyUiState(
            loaded = true,
            levels = levels,
            unlocked = Learning.unlockedLessons(order, status),
            passed = status.filterValues { Learning.isPassed(it) }.keys,
            stars = status.mapValues { it.value.bestStars },
            nextLesson = levels.flatMap { it.lessons }.firstOrNull { it.id == nextId },
            dueCount = due,
            streak = streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JourneyUiState())

    companion object {
        fun factory(childId: Long, content: ContentRepository, progress: ProgressRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { JourneyViewModel(childId, content, progress) } }
    }
}
