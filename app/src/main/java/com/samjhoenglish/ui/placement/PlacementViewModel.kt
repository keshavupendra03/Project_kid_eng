package com.samjhoenglish.ui.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.content.ContentRepository
import com.samjhoenglish.content.LevelInfo
import com.samjhoenglish.content.PlacementSection
import com.samjhoenglish.content.Step
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.learning.Learning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlacementPhase { INTRO, QUESTIONS, RESULT }

data class PlacementUiState(
    val phase: PlacementPhase = PlacementPhase.INTRO,
    val sections: List<PlacementSection> = emptyList(),
    val section: Int = 0,
    val position: Int = 0,
    val rightInSection: Int = 0,
    val answered: Boolean = false,
    val startLevel: LevelInfo? = null,
) {
    val current: Step? get() = sections.getOrNull(section)?.steps?.getOrNull(position)
    val progress: Float
        get() {
            val total = sections.sumOf { it.steps.size }
            val done = sections.take(section).sumOf { it.steps.size } + position
            return if (total == 0) 0f else done.toFloat() / total
        }
}

/**
 * Runs the placement quiz level by level. A level counts as "known" with 80% right;
 * the quiz stops at the first level that isn't known, and the child starts there.
 */
class PlacementViewModel(
    private val childId: Long,
    private val content: ContentRepository,
    private val progress: ProgressRepository,
) : ViewModel() {
    private val state = MutableStateFlow(PlacementUiState())
    val uiState: StateFlow<PlacementUiState> = state.asStateFlow()

    private var levels: List<LevelInfo> = emptyList()
    private val knownLevels = mutableSetOf<Int>()

    init {
        viewModelScope.launch {
            levels = content.levels()
            // Only test a level if there is a later level with lessons to move on to.
            val lastLevelWithLessons = levels.filter { it.lessons.isNotEmpty() }.maxOfOrNull { it.number } ?: 0
            val sections = content.placement().sections
                .filter { it.level < lastLevelWithLessons && it.steps.isNotEmpty() }
                .sortedBy { it.level }
            state.value = state.value.copy(sections = sections)
        }
    }

    fun start() {
        if (state.value.sections.isEmpty()) finish() else state.value = state.value.copy(phase = PlacementPhase.QUESTIONS)
    }

    /** "Start from the beginning": no quiz, begin at Level 0. */
    fun skip() = finish()

    fun answer(correct: Boolean) {
        val s = state.value
        if (s.answered) return
        state.value = s.copy(answered = true, rightInSection = s.rightInSection + if (correct) 1 else 0)
    }

    fun next() {
        val s = state.value
        val section = s.sections.getOrNull(s.section) ?: return
        if (s.position + 1 < section.steps.size) {
            state.value = s.copy(position = s.position + 1, answered = false)
            return
        }
        val passed = s.rightInSection.toFloat() / section.steps.size >= Learning.PLACEMENT_PASS
        if (passed) knownLevels += section.level
        if (passed && s.section + 1 < s.sections.size) {
            state.value = s.copy(section = s.section + 1, position = 0, rightInSection = 0, answered = false)
        } else {
            finish()
        }
    }

    private fun finish() {
        viewModelScope.launch {
            progress.applyPlacement(childId, knownLevels)
            val start = knownLevels.maxOrNull()?.plus(1) ?: 0
            state.value = state.value.copy(
                phase = PlacementPhase.RESULT,
                startLevel = levels.firstOrNull { it.number == start } ?: levels.firstOrNull(),
            )
        }
    }

    companion object {
        fun factory(childId: Long, content: ContentRepository, progress: ProgressRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlacementViewModel(childId, content, progress) } }
    }
}
