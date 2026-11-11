package com.samjhoenglish.ui.express

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.ExpressLevel
import com.samjhoenglish.content.LessonRef
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.learning.Express
import com.samjhoenglish.learning.Learning
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class ExpressUiState(
    val loaded: Boolean = false,
    val levels: List<ExpressLevel> = emptyList(),
    val unlocked: Set<String> = emptySet(),
    val stars: Map<String, Int> = emptyMap(),
    val bestWpm: Int? = null,
    /** Reading stories already passed, to re-read faster. */
    val bonus: List<LessonRef> = emptyList(),
)

class ExpressViewModel(childId: Long, content: ContentRepository, progress: ProgressRepository) : ViewModel() {
    val uiState: StateFlow<ExpressUiState> = combine(
        flow { emit(content.express()) },
        flow { emit(content.readingLevels()) },
        progress.lessonStatus(childId),
        progress.expressBestWpm(childId),
    ) { levels, reading, status, best ->
        val stars = status.mapValues { it.value.bestStars }
        ExpressUiState(
            loaded = true,
            levels = levels,
            unlocked = Express.unlocked(levels.map { level -> level.passages.map { it.id } }, stars),
            stars = stars,
            bestWpm = best,
            bonus = reading.flatMap { it.lessons }.filter { Learning.isPassed(status[it.id]) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpressUiState())

    companion object {
        fun factory(childId: Long, content: ContentRepository, progress: ProgressRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { ExpressViewModel(childId, content, progress) } }
    }
}
